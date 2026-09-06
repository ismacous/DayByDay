package com.ismael.daybyday.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Version du schema. Affichee dans les reglages, a propos, pour savoir ce que
 * fait tourner le telephone en cas de probleme.
 */
const val DATABASE_VERSION = 13

@Database(
    entities = [
        DayEntry::class,
        MediaItem::class,
        Tag::class,
        DayTagCrossRef::class,
        MoneyEntry::class,
        Treatment::class,
        DoseTaken::class,
    ],
    version = DATABASE_VERSION,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun dayDao(): DayDao

    companion object {
        private const val NAME = "daybyday.db"

        /** Etiquettes proposees au premier lancement ; tout est modifiable ensuite. */
        /** emoji, nom, famille. */
        val DEFAULT_TAGS = listOf(
            Triple("😴", "Bien dormi", TagCategory.SLEEP),
            Triple("🥱", "Mal dormi", TagCategory.SLEEP),
            Triple("👥", "Ami·es", TagCategory.SOCIAL),
            Triple("🏠", "Famille", TagCategory.SOCIAL),
            Triple("💬", "Copine", TagCategory.SOCIAL),
            Triple("🚶", "Marche", TagCategory.ACTIVITY),
            Triple("🌳", "Dehors / nature", TagCategory.ACTIVITY),
            Triple("🍳", "Cuisine maison", TagCategory.FOOD),
            Triple("🍟", "Fast-food", TagCategory.FOOD),
            Triple("💼", "Recherche d'emploi", TagCategory.WORK),
            Triple("📱", "Écrans +++", TagCategory.SCREENS),
            Triple("🎮", "Jeux vidéo", TagCategory.SCREENS),
        )

        /** Insertion pour une base fraiche, qui possede deja la colonne category. */
        private fun seedTags(db: SupportSQLiteDatabase) {
            DEFAULT_TAGS.forEachIndexed { index, (emoji, name, category) ->
                db.execSQL(
                    "INSERT INTO tags (name, emoji, sortOrder, category) VALUES (?, ?, ?, ?)",
                    arrayOf<Any>(name, emoji, index, category.key),
                )
            }
        }

        /**
         * Insertion au format de la version 2 : la colonne category n'existe pas
         * encore a ce stade, elle est ajoutee par la migration suivante.
         */
        private fun seedTagsWithoutCategory(db: SupportSQLiteDatabase) {
            DEFAULT_TAGS.forEachIndexed { index, (emoji, name, _) ->
                db.execSQL(
                    "INSERT INTO tags (name, emoji, sortOrder) VALUES (?, ?, ?)",
                    arrayOf<Any>(name, emoji, index),
                )
            }
        }

        /** Range les etiquettes deja creees dans leur famille. */
        private fun categorizeExistingTags(db: SupportSQLiteDatabase) {
            DEFAULT_TAGS.forEach { (_, name, category) ->
                db.execSQL(
                    "UPDATE tags SET category = ? WHERE name = ? AND category IS NULL",
                    arrayOf<Any>(category.key, name),
                )
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE day_entries ADD COLUMN sportLevel INTEGER")
                db.execSQL("ALTER TABLE day_entries ADD COLUMN foodLevel INTEGER")
                db.execSQL("ALTER TABLE day_entries ADD COLUMN wentOut INTEGER")
                db.execSQL("ALTER TABLE day_entries ADD COLUMN weightKg REAL")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `tags` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`emoji` TEXT NOT NULL, " +
                        "`sortOrder` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `day_tags` (" +
                        "`epochDay` INTEGER NOT NULL, " +
                        "`tagId` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`epochDay`, `tagId`))"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_day_tags_tagId` ON `day_tags` (`tagId`)")
                seedTagsWithoutCategory(db)
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE day_entries ADD COLUMN partMorning INTEGER")
                db.execSQL("ALTER TABLE day_entries ADD COLUMN partAfternoon INTEGER")
                db.execSQL("ALTER TABLE day_entries ADD COLUMN partEvening INTEGER")
                db.execSQL("ALTER TABLE day_entries ADD COLUMN partNight INTEGER")
                db.execSQL("ALTER TABLE day_entries ADD COLUMN colorManual INTEGER")
                db.execSQL("UPDATE day_entries SET colorManual = 1 WHERE colorKey IS NOT NULL")
                db.execSQL("ALTER TABLE tags ADD COLUMN category TEXT")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `transactions` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`epochDay` INTEGER NOT NULL, " +
                        "`amountCents` INTEGER NOT NULL, " +
                        "`label` TEXT NOT NULL, " +
                        "`categoryKey` TEXT, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_transactions_epochDay` " +
                        "ON `transactions` (`epochDay`)"
                )
                categorizeExistingTags(db)
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE day_entries ADD COLUMN steps INTEGER")
                db.execSQL("ALTER TABLE day_entries ADD COLUMN screenMinutes INTEGER")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tags ADD COLUMN slug TEXT")
            }
        }

        /**
         * Les corrections de solde etaient enregistrees comme des mouvements
         * ordinaires : une correction de +14,90 apparaissait donc comme une
         * rentree de 14,90 dans le bilan du mois. Elles recoivent leur propre
         * categorie pour etre comptees a part. Aucune colonne ne change, seules
         * les lignes deja enregistrees sont marquees.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "UPDATE transactions SET categoryKey = ? " +
                        "WHERE categoryKey IS NULL AND label = ?",
                    arrayOf<Any>(MoneyCategory.ADJUSTMENT.key, MoneyCategory.ADJUSTMENT_LABEL),
                )
            }
        }

        /**
         * L'ecran d'une journee devient modulaire : le sommeil, l'eau, ce qui a
         * ete mange et les traitements arrivent. Les colonnes s'ajoutent vides,
         * donc les journees deja ecrites restent telles quelles.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE day_entries ADD COLUMN sleepStartMinutes INTEGER")
                db.execSQL("ALTER TABLE day_entries ADD COLUMN sleepEndMinutes INTEGER")
                db.execSQL("ALTER TABLE day_entries ADD COLUMN sleepFromDevice INTEGER")
                db.execSQL("ALTER TABLE day_entries ADD COLUMN waterGlasses INTEGER")
                db.execSQL(
                    "ALTER TABLE day_entries ADD COLUMN mealsNote TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `treatments` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`dose` TEXT NOT NULL, " +
                        "`timesMask` INTEGER NOT NULL, " +
                        "`active` INTEGER NOT NULL, " +
                        "`sortOrder` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `doses_taken` (" +
                        "`epochDay` INTEGER NOT NULL, " +
                        "`treatmentId` INTEGER NOT NULL, " +
                        "`timeKey` INTEGER NOT NULL, " +
                        "`takenAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`epochDay`, `treatmentId`, `timeKey`))"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_doses_taken_epochDay` " +
                        "ON `doses_taken` (`epochDay`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_doses_taken_treatmentId` " +
                        "ON `doses_taken` (`treatmentId`)"
                )
            }
        }

        /**
         * Une photo peut desormais etre rattachee a une carte. Les medias deja
         * enregistres restent sans carte : ils appartiennent a la journee, et
         * s'affichent dans "Photos & videos" comme avant.
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE media_items ADD COLUMN cardKey TEXT")
            }
        }

        /**
         * Le journal accepte la mise en forme. Elle est rangee a part du texte,
         * donc les journees deja ecrites restent lisibles telles quelles : sans
         * intervalle, elles s'affichent simplement sans decoration.
         */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE day_entries ADD COLUMN noteSpans TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        /**
         * Les photos se posent librement sur la page du journal : position,
         * taille, inclinaison, calque et forme. Les photos deja la n'ont pas
         * de position (colonnes nulles) : elles seront rangees automatiquement
         * a la premiere ouverture, aucune n'est perdue.
         */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE media_items ADD COLUMN placedX REAL")
                db.execSQL("ALTER TABLE media_items ADD COLUMN placedY REAL")
                db.execSQL(
                    "ALTER TABLE media_items ADD COLUMN placedWidth REAL NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE media_items ADD COLUMN placedHeight REAL NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE media_items ADD COLUMN placedRotation REAL NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE media_items ADD COLUMN layerKey INTEGER NOT NULL DEFAULT 2"
                )
                db.execSQL(
                    "ALTER TABLE media_items ADD COLUMN shapeKey INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        /**
         * Les autocollants : un PNG detoure garde sa silhouette, et peut porter
         * un contour blanc. Les photos deja posees n'en ont pas.
         */
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE media_items ADD COLUMN stickerOutline INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        /**
         * Les cinq prieres du jour, en masque de bits dans une seule colonne.
         *
         * La colonne accepte `null`, et c'est voulu : une journee d'avant cette
         * version n'est pas une journee sans priere, c'est une journee dont on
         * ne sait rien. Mettre zero partout inventerait des reponses.
         */
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE day_entries ADD COLUMN prayerMask INTEGER")
            }
        }

        /**
         * Le grignotage en texte libre, et les candidatures du jour.
         *
         * `snackNote` est NOT NULL avec une valeur par defaut : c'est un texte,
         * et « pas de texte » se dit avec une chaine vide. `jobApplications`
         * accepte `null` au contraire, parce que zero candidature et « je n'ai
         * pas rempli » ne veulent pas dire la meme chose.
         */
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE day_entries ADD COLUMN snackNote TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL("ALTER TABLE day_entries ADD COLUMN jobApplications INTEGER")
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                NAME,
            )
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                    MIGRATION_10_11,
                    MIGRATION_11_12,
                    MIGRATION_12_13,
                )
                .build()
                .also { instance = it }
        }
    }
}
