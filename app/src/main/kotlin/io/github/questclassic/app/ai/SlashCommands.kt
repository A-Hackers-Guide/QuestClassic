package io.github.questclassic.app.ai

/**
 * Parser + registry for in-chat slash commands the user can run from
 * the AI panel text input: `/help`, `/clear`, `/switch claude`, `/export`,
 * `/status`, etc. The AI panel calls [parse]; if the result is non-null,
 * the panel executes it locally instead of sending to the model.
 *
 * Pure function, unit-testable; the panel's own glue code binds each
 * [Command] to actual side-effects (clear history, switch backend, …).
 */
object SlashCommands {

    data class Parsed(
        val command: Command,
        /** Arguments after the command name, trimmed and split on whitespace. */
        val args: List<String>,
        /** The original input (without leading `/`). */
        val raw: String,
    )

    /** Canonical set of commands. */
    enum class Command(val keyword: String, val help: String) {
        HELP("help", "list commands"),
        CLEAR("clear", "clear the current chat history"),
        STATUS("status", "show the active backend / model / name"),
        SWITCH("switch", "switch active backend: /switch {claude|openai|gemini|ollama}"),
        EXPORT("export", "export this chat to a JSON file"),
        RENAME("rename", "rename the active assistant: /rename Nova"),
        WHOAMI("whoami", "shortcut for /status"),
        RESET("reset", "reset the first-run wizard"),
        ;

        companion object {
            fun byKeyword(k: String): Command? =
                entries.firstOrNull { it.keyword.equals(k, ignoreCase = true) }
        }
    }

    /**
     * @return [Parsed] when the input is a slash command we know; null
     * otherwise (so the panel passes the input to the model as normal).
     */
    fun parse(input: String): Parsed? {
        val t = input.trimStart()
        if (!t.startsWith("/")) return null
        val body = t.substring(1).trim()
        if (body.isEmpty()) return null
        val tokens = body.split(Regex("\\s+"))
        val keyword = tokens.first()
        val cmd = Command.byKeyword(keyword) ?: return null
        val args = tokens.drop(1).filter { it.isNotBlank() }
        return Parsed(command = cmd, args = args, raw = body)
    }

    /** Lookup renders in the `/help` response. */
    fun helpLines(): List<String> = Command.entries.map { "/${it.keyword} — ${it.help}" }
}
