import type { Section } from './types'

export const command: Section = {
  id: 'command',
  why: {
    ru: [
      'Paper перешёл на Brigadier, и регистрация команд теперь идёт через lifecycle-события в onLoad. Автодополнение по аргументам приходится описывать отдельно от выполнения.',
      'EffectiveCommand регистрирует команду сам и берёт одно дерево CommandNode и для выполнения, и для автодополнения: choice — литерал, dynamic — список на лету, executes — что делать.',
    ],
    en: [
      'Paper moved to Brigadier, and command registration now goes through lifecycle events in onLoad. Argument completion has to be described separately from execution.',
      'EffectiveCommand registers the command itself and uses one CommandNode tree for both execution and completion: choice is a literal, dynamic a runtime list, executes the action.',
    ],
  },
  examples: [
    {
      title: { ru: 'Команда', en: 'Command' },
      code: `object ArenaCommand : EffectiveCommand() {
    override fun getNamespacedData() = MyPlugin.instance to "arena"
    override fun getDescription() = "Arena control"
    override fun getPermission() = "myplugin.arena"

    override fun commandTree() = CommandNode.build {
        choice("start") {
            executes { Arena.start(); sendMessage("started") }
        }
        choice("kick") {
            dynamic({ Bukkit.getOnlinePlayers().map { it.name } }) {
                executes { args -> Bukkit.getPlayer(args[1])?.kick() }
            }
        }
    }

    fun init() {}
}`,
      note: {
        ru: 'ArenaCommand.init() вызывается из onLoad.',
        en: 'ArenaCommand.init() is called from onLoad.',
      },
    },
  ],
  methods: [
    { name: 'getNamespacedData()', required: true, desc: { ru: 'Плагин и имя команды', en: 'Plugin and command name' } },
    { name: 'getDescription()', required: true, desc: { ru: 'Описание для /help', en: 'Description for /help' } },
    { name: 'getPermission()', required: true, desc: { ru: 'Право; пустая строка — без права', en: 'Permission; empty string — none' } },
    { name: 'commandTree()', required: true, desc: { ru: 'Дерево аргументов', en: 'Argument tree' } },
  ],
  pitfalls: {
    ru: [
      'Создали команду в onEnable — её нет. Только onLoad.',
      'executes выполняется на самом глубоком совпавшем узле; args — весь массив аргументов с нуля.',
    ],
    en: [
      'Created the command in onEnable — it does not exist. onLoad only.',
      'executes runs on the deepest matching node; args is the whole argument array from zero.',
    ],
  },
}
