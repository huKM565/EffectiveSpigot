import type { Section } from './types'

export const screen: Section = {
  id: 'screen',
  why: {
    ru: [
      'Затемнение экрана и тряска камеры — приёмы из катсцен: спрятать телепорт, показать взрыв, оглушить. В Bukkit первого нет вовсе, второе делается телепортами и дёргает игрока.',
      'EffectiveScreenEffects рисует затемнение полноэкранным глифом из пака фреймворка, а тряску шлёт пакетом относительного поворота — камера дрожит, игрок не двигается.',
      'EffectiveScreenImage и EffectiveScreenText идут дальше: картинка или текст в любой точке экрана. Пак фреймворка подменяет core-шейдер текста, а координаты и размер передаются в цвете символа — клиенту не нужно ничего, кроме ресурспака.',
    ],
    en: [
      'Screen fade and camera shake are cutscene tools: hide a teleport, sell an explosion, stun. Bukkit has no fade at all, and shake is done with teleports that jerk the player.',
      'EffectiveScreenEffects draws the fade with a full-screen glyph from the framework pack and sends shake as a relative rotation packet — the camera trembles, the player does not move.',
      'EffectiveScreenImage and EffectiveScreenText go further: an image or text at any point of the screen. The framework pack overrides the text core shader, and position and size travel in the character colour — the client needs nothing but the resource pack.',
    ],
  },
  examples: [
    {
      title: { ru: 'Затемнение с телепортом', en: 'Fade with teleport' },
      code: `EffectiveScreenEffects.runCameraFade(player, fadeIn = 10, stay = 20, fadeOut = 10) {
    player.teleport(arenaSpawn)
}`,
    },
    {
      title: { ru: 'Тряска', en: 'Shake' },
      code: `EffectiveScreenEffects.runCameraShake(player, intensity = 3f, duration = 40, type = ShakeType.EASE_OUT)

EffectiveScreenEffects.runCameraShake(player, intensity = 1f) { Arena.isRunning }`,
    },
    {
      title: { ru: 'Картинка и текст на экране', en: 'Image and text on screen' },
      code: `object Logo : EffectiveScreenImage() {
    override fun getNamespacedData() = MyPlugin.instance to "logo"
    override fun getTexturePath() = "textures/screen/logo.png"
    override fun getX() = 0.05f
    override fun getY() = 0.05f
    override fun getSize() = 0.25f
    fun init() {}
}

Logo.show(player)
Logo.show(player, durationTicks = 100)
Logo.hide(player)

EffectiveScreenText.show(player, "Wave 3", x = 0.5f, y = 0.1f, scale = 2f)`,
      note: {
        ru: 'x, y — доля экрана от левого верхнего угла, size — доля ширины экрана. PNG любой формы: фреймворк сам дополняет до квадрата.',
        en: 'x, y are screen fractions from the top-left corner, size is a fraction of the screen width. Any PNG shape: the framework pads it to a square itself.',
      },
    },
  ],
  methods: [
    { name: 'getNamespacedData()', required: true, desc: { ru: 'EffectiveScreenImage: плагин и id', en: 'EffectiveScreenImage: plugin and id' } },
    { name: 'getTexturePath()', required: true, desc: { ru: 'PNG внутри jar плагина', en: 'PNG inside the plugin jar' } },
    { name: 'getX() / getY()', required: false, default: '0f', desc: { ru: 'Позиция по умолчанию, доли экрана', en: 'Default position, screen fractions' } },
    { name: 'getSize()', required: false, default: '0.25f', desc: { ru: 'Ширина по умолчанию, доля экрана', en: 'Default width, screen fraction' } },
  ],
  pitfalls: {
    ru: [
      'EffectiveScreenImage и EffectiveScreenText — экспериментальные: техника через core-шейдер, API и поведение могут поменяться. Затемнение и тряска стабильны.',
      'Всё это — title. Требует включённого ресурспака фреймворка; без пака игрок увидит квадратик или голый символ.',
      'Один title на игрока: картинка, текст и затемнение перебивают друг друга. Несколько элементов сразу — склеивайте компоненты в один title через getComponent().',
      'Шейдерпаки (Iris, OptiFine) подменяют core-шейдеры — у таких игроков картинка/текст встанут не туда. Затемнение и тряска работают всегда.',
      'Тряска заканчивается компенсирующим поворотом — камера возвращается туда, где была.',
    ],
    en: [
      'EffectiveScreenImage and EffectiveScreenText are experimental: the core-shader technique, API and behaviour may change. Fade and shake are stable.',
      'All of it is a title. Needs the framework resource pack enabled; without it the player sees a box or a bare character.',
      'One title per player: image, text and fade override each other. For several elements at once, join the components into one title via getComponent().',
      'Shader packs (Iris, OptiFine) replace core shaders — for those players the image/text will land in the wrong place. Fade and shake always work.',
      'Shake ends with a compensating rotation — the camera returns to where it was.',
    ],
  },
}
