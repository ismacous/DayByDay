package com.ismael.daybyday

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ismael.daybyday.data.AppDatabase
import com.ismael.daybyday.data.MoneyCategory
import com.ismael.daybyday.data.Stats
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifie que les journees deja enregistrees avec la version 1 de la base
 * (avant les categories sport / alimentation / sorties et les etiquettes)
 * sont bien conservees apres la mise a jour de l'application.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val databaseName = "migration-test.db"

    @Test
    fun lesDonneesDeLaVersion1SurviventALaMigration() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = context.getDatabasePath(databaseName)
        file.parentFile?.mkdirs()
        context.deleteDatabase(databaseName)

        // Base telle que la version 1 de l'application la creait.
        val legacy = SQLiteDatabase.openOrCreateDatabase(file, null)
        legacy.execSQL(
            "CREATE TABLE IF NOT EXISTS `day_entries` (" +
                "`epochDay` INTEGER NOT NULL, `colorKey` INTEGER, `title` TEXT NOT NULL, " +
                "`note` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`epochDay`))"
        )
        legacy.execSQL(
            "CREATE TABLE IF NOT EXISTS `media_items` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `epochDay` INTEGER NOT NULL, " +
                "`relativePath` TEXT NOT NULL, `kindKey` INTEGER NOT NULL, `addedAt` INTEGER NOT NULL)"
        )
        legacy.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_media_items_epochDay` ON `media_items` (`epochDay`)"
        )
        legacy.execSQL(
            "INSERT INTO day_entries (epochDay, colorKey, title, note, updatedAt) " +
                "VALUES (20000, 2, 'Retour à la maison', 'Hier je suis allé chez Dune', 1)"
        )
        legacy.execSQL(
            "INSERT INTO media_items (epochDay, relativePath, kindKey, addedAt) " +
                "VALUES (20000, '2026/09/photo.jpg', 0, 1)"
        )
        legacy.version = 1
        legacy.close()

        val database = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6,
                AppDatabase.MIGRATION_6_7,
                AppDatabase.MIGRATION_7_8,
                AppDatabase.MIGRATION_8_9,
                AppDatabase.MIGRATION_9_10,
                AppDatabase.MIGRATION_10_11,
                AppDatabase.MIGRATION_11_12,
                AppDatabase.MIGRATION_12_13,
                AppDatabase.MIGRATION_13_14,
                AppDatabase.MIGRATION_14_15,
                AppDatabase.MIGRATION_15_16,
                AppDatabase.MIGRATION_16_17,
                AppDatabase.MIGRATION_17_18,
                AppDatabase.MIGRATION_18_19,
                AppDatabase.MIGRATION_19_20,
                AppDatabase.MIGRATION_20_21,
                AppDatabase.MIGRATION_21_22,
            )
            .build()

        try {
            runBlocking {
                val dao = database.dayDao()
                val day = dao.dayOnce(20000)
                assertEquals("Retour à la maison", day?.title)
                assertEquals(2, day?.colorKey)
                // La page ecrite en version 1 est devenue un bloc de texte :
                // c'est la migration 17->18 qui l'a decoupee, et sans elle la
                // journee s'ouvrirait sur une page vide.
                val blocks = dao.blocksForDay(20000)
                assertEquals(1, blocks.size)
                assertEquals("Hier je suis allé chez Dune", blocks.first().text)
                assertEquals(
                    com.ismael.daybyday.data.BlockKind.TEXT,
                    blocks.first().kind,
                )
                // Les nouveaux champs existent et sont simplement vides.
                assertEquals(null, day?.sportLevel)
                assertEquals(null, day?.weightKg)
                // Une journee d'avant les cartes verifiees arrive « pas encore
                // verifiee », et non « tout verifie » : la colonne est vide, et
                // la marque se pose ensuite comme sur n'importe quelle journee.
                assertEquals("", day?.checkedCards)
                assertEquals(emptySet<String>(), day?.checkedCardKeys)
                dao.upsertDay(day!!.withCheckedCard("priere", true))
                assertEquals(setOf("priere"), dao.dayOnce(20000)?.checkedCardKeys)
                dao.upsertDay(dao.dayOnce(20000)!!.withCheckedCard("priere", false))
                assertEquals(emptySet<String>(), dao.dayOnce(20000)?.checkedCardKeys)
                assertEquals(1, dao.mediaForDay(20000).size)
                // La photo d'origine survit et n'a pas encore de place sur la
                // page : elle sera rangee a la premiere ouverture du journal.
                val media = dao.mediaForDay(20000).first()
                assertEquals(null, media.placedX)
                assertEquals(false, media.isPlaced)
                assertEquals(com.ismael.daybyday.data.MediaLayer.FRONT, media.layer)
                assertEquals(com.ismael.daybyday.data.MediaShape.RECTANGLE, media.shape)
                assertEquals(false, media.stickerOutline)
                // Les etiquettes par defaut sont ajoutees par la migration,
                // et rangees dans leur famille par la suivante.
                val tags = dao.allTags()
                assertTrue(tags.isNotEmpty())
                assertTrue(tags.any { it.category != null })
                // La colonne d'identifiant stable existe (vide avant synchronisation).
                assertEquals(null, tags.first().slug)
                // La table des mouvements d'argent est utilisable.
                dao.upsertMoney(
                    com.ismael.daybyday.data.MoneyEntry(
                        epochDay = 20000,
                        amountCents = -1250,
                        label = "Test",
                    )
                )
                assertEquals(1, dao.allMoney().size)
                // Les moments de la journee existent et sont vides.
                assertEquals(null, day?.partMorning)
                assertEquals(null, day?.steps)
                assertEquals(null, day?.screenMinutes)
                assertEquals(true, day?.colorManual)
                // Les prieres : une journee d'avant n'est pas une journee sans
                // priere, c'est une journee dont on ne sait rien.
                assertEquals(null, day?.prayerMask)
                // Le grignotage est un texte : « rien » se dit par une chaine
                // vide. Les candidatures sont un nombre : « rien » se dit par
                // null, parce que zero candidature est une reponse.
                assertEquals("", day?.snackNote)
                assertEquals(null, day?.jobApplications)
                assertEquals("", day?.medicalNote)
                assertEquals("", day?.medicalWith)
                // La table des vocaux existe et repond, meme si aucune journee
                // d'avant n'en contient : c'est ce qui prouve que la migration
                // l'a bien creee, et pas seulement que Room ne s'est pas plaint.
                assertEquals(0, dao.voiceNotesForDay(20000).size)
                dao.insertVoiceNote(
                    com.ismael.daybyday.data.VoiceNote(
                        epochDay = 20000,
                        relativePath = "2026/09/vocal.m4a",
                        durationMs = 4200,
                    )
                )
                assertEquals(1, dao.voiceNotesForDay(20000).size)
                // Un vocal qui vient d'etre enregistre n'a pas encore de place
                // sur la page : il sera range a la prochaine ouverture du
                // journal, exactement comme une photo.
                val voice = dao.voiceNotesForDay(20000).first()
                assertEquals(false, voice.isPlaced)
                assertEquals(true, voice.wide)
                assertEquals("", voice.waveform)
                dao.updateVoiceNote(voice.copy(placedX = 14f, placedY = 28f, wide = false))
                val moved = dao.voiceNotesForDay(20000).first()
                assertEquals(true, moved.isPlaced)
                assertEquals(false, moved.wide)
            }
        } finally {
            database.close()
            context.deleteDatabase(databaseName)
        }
    }

    /**
     * Les corrections de solde enregistrees avant la version 6 passaient pour
     * de vraies rentrees dans le bilan du mois. La migration les marque pour
     * qu'elles soient comptees a part, sans toucher aux vrais mouvements.
     *
     * La base est d'abord creee par Room, donc avec le bon schema, puis
     * ramenee a la version 5 : c'est exactement la situation d'un telephone
     * qui installe la mise a jour.
     */
    @Test
    fun lesAnciennesCorrectionsDeSoldeSontMarquees() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(databaseName)

        Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .build()
            .apply { runBlocking { dayDao().allMoney() } }
            .close()

        // Retour a l'etat d'avant la mise a jour : version 5, et des
        // corrections de solde enregistrees comme des mouvements ordinaires.
        val file = context.getDatabasePath(databaseName)
        val legacy = SQLiteDatabase.openOrCreateDatabase(file, null)
        legacy.execSQL(
            "INSERT INTO transactions (epochDay, amountCents, label, categoryKey, createdAt) " +
                "VALUES (20000, 2826, 'Ajustement du solde', NULL, 1)"
        )
        legacy.execSQL(
            "INSERT INTO transactions (epochDay, amountCents, label, categoryKey, createdAt) " +
                "VALUES (20000, -1490, 'Pizza + milkshake', 'courses', 1)"
        )
        legacy.version = 5
        legacy.close()

        val database = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .addMigrations(AppDatabase.MIGRATION_5_6)
            .build()

        try {
            runBlocking {
                val money = database.dayDao().allMoney()
                val correction = money.first { it.label == "Ajustement du solde" }
                val depense = money.first { it.label == "Pizza + milkshake" }

                assertTrue(correction.isAdjustment)
                assertTrue(!depense.isAdjustment)
                assertEquals(MoneyCategory.FOOD, depense.category)

                // La correction ne pese plus dans les rentrees du mois.
                val summary = Stats.summarizeMoney(money)
                assertEquals(0L, summary.incomeCents)
                assertEquals(1490L, summary.spentCents)
                assertEquals(2826L, summary.adjustmentCents)
            }
        } finally {
            database.close()
            context.deleteDatabase(databaseName)
        }
    }
}