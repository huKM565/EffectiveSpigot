import type { Section } from './types'

export const advancement: Section = {
  id: 'advancement',
  why: {
    ru: [
      'Своя вкладка достижений — это json-датапак с критериями, которые почти никогда не описывают то, что вам нужно. Обычно хочется просто «выдать ачивку, когда плагин решил».',
      'EffectiveAdvancement — только визуальный слой: вкладка, дерево, тост, рамка. Критериев нет, когда выдать — решает плагин вызовом grant.',
    ],
    en: [
      'Your own advancement tab is a json datapack with criteria that almost never describe what you need. Usually you just want to "grant the advancement when the plugin decides".',
      'EffectiveAdvancement is the visual layer only: tab, tree, toast, frame. No criteria — the plugin decides when by calling grant.',
    ],
  },
  examples: [
    {
      title: { ru: 'Корень и ребёнок', en: 'Root and child' },
      code: `object RootAdvancement : EffectiveAdvancement() {
    override fun getNamespacedData() = MyPlugin.instance to "root"
    override fun getParent() = null
    override fun getDisplay() = DisplayData(
        title = "My plugin",
        description = "Start",
        icon = IconData.fromItem(RubyItem.createItemStack()),
        frame = FrameType.TASK,
        background = "minecraft:textures/block/stone.png",
    )
    fun init() {}
}

object FirstRuby : EffectiveAdvancement() {
    override fun getNamespacedData() = MyPlugin.instance to "first_ruby"
    override fun getParent() = NamespacedKey(MyPlugin.instance, "root")
    override fun getDisplay() = DisplayData(
        title = "First ruby",
        description = "Find a ruby",
        icon = IconData(Material.EMERALD, hashMapOf()),
        frame = FrameType.GOAL,
    )
    fun init() {}
}

if (!FirstRuby.isGrantedTo(player)) FirstRuby.grant(player)`,
    },
  ],
  methods: [
    { name: 'getNamespacedData()', required: true, desc: { ru: 'Плагин и id', en: 'Plugin and id' } },
    { name: 'getParent()', required: true, desc: { ru: 'Ключ родителя или null для корня', en: 'Parent key or null for the root' } },
    { name: 'getDisplay()', required: true, desc: { ru: 'Заголовок, описание, иконка, рамка, фон, toast, анонс, скрытость', en: 'Title, description, icon, frame, background, toast, announce, hidden' } },
  ],
  pitfalls: {
    ru: [
      'У корня обязателен background — иначе вкладка без фона.',
      'Родитель должен быть инициализирован — регистрация идёт в порядке родитель → ребёнок.',
      'grant без проверки isGrantedTo — повторный тост не покажется, но и вреда нет.',
    ],
    en: [
      'The root needs a background — otherwise the tab has none.',
      'The parent must be initialised — registration goes parent → child.',
      'grant without checking isGrantedTo — the toast will not repeat, but there is no harm either.',
    ],
  },
}
