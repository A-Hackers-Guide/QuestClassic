package io.github.questclassic.app.core

/**
 * Ordered schema migrations for [ConfigBackup].
 *
 * Right now backup is v1; this scaffolds the migration chain so a
 * future v2 doesn't scramble an installed user's v1 config. Each
 * migration is a pure function `(JsonElement, from version) -> JsonElement`.
 *
 * Why not just field-rename in ConfigBackup.kt: because once backup
 * files travel, we can't assume every incoming file has been updated
 * to the latest schema. The import path runs the migrator against the
 * blob's declared version to bring it up to CURRENT_VERSION before the
 * schema-aware decoder runs.
 *
 * Pure function; unit-testable without DataStore.
 */
class ConfigMigrator(
    private val migrations: List<Migration> = BUILT_IN,
) {
    data class Migration(val fromVersion: Int, val toVersion: Int, val apply: (String) -> String) {
        init {
            require(toVersion == fromVersion + 1) {
                "migrations must step by 1; got $fromVersion → $toVersion"
            }
        }
    }

    /** Apply the chain of migrations from [fromVersion] to [toVersion].
     *  If no path exists, throws. */
    fun migrate(json: String, fromVersion: Int, toVersion: Int): String {
        if (fromVersion == toVersion) return json
        require(fromVersion < toVersion) {
            "downgrade not supported: $fromVersion → $toVersion"
        }
        var current = json
        for (v in fromVersion until toVersion) {
            val m = migrations.firstOrNull { it.fromVersion == v }
                ?: throw IllegalStateException("no migration v$v → v${v + 1}")
            current = m.apply(current)
        }
        return current
    }

    companion object {
        /** Empty today; when v2 lands, add a migration here that
         *  translates v1 to v2 and update the test suite to cover it. */
        val BUILT_IN: List<Migration> = emptyList()
    }
}
