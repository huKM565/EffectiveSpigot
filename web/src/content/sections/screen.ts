import type { Section } from './types'

export const screen: Section = {
  id: 'screen',
  why: {
    ru: [
      'Затемнение экрана и тряска камеры — приёмы из катсцен: спрятать телепорт, показать взрыв, оглушить. В Bukkit первого нет вовсе, второе делается телепортами и дёргает игрока.',
      'EffectiveScreenEffects рисует затемнение полноэкранным глифом из пака фреймворка, а тряску шлёт пакетом относительного поворота — камера дрожит, игрок не двигается.',
      'EffectiveExperienceBar рисует любой прогресс в полоске опыта — индикатор заряда или каста. Опыт игрока не меняется: полоска обновляется пакетом, reset возвращает настоящую.',
      'EffectiveScreenImage и EffectiveScreenText идут дальше: картинка или текст в любой точке экрана. Пак фреймворка подменяет core-шейдер текста, а координаты и размер передаются в цвете символа — клиенту не нужно ничего, кроме ресурспака.',
    ],
    en: [
      'Screen fade and camera shake are cutscene tools: hide a teleport, sell an explosion, stun. Bukkit has no fade at all, and shake is done with teleports that jerk the player.',
      'EffectiveScreenEffects draws the fade with a full-screen glyph from the framework pack and sends shake as a relative rotation packet — the camera trembles, the player does not move.',
      'EffectiveExperienceBar draws any progress into the experience bar — a charge or cast indicator. The experience the player owns is untouched: the bar is updated by packet and reset puts the real one back.',
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
      title: { ru: 'Полоска заряда', en: 'Charge bar' },
      code: `EffectiveExperienceBar.show(player, charge / 60f)

EffectiveExperienceBar.reset(player)`,
      note: {
        ru: 'Полоску прыжка лошади сервером не нарисовать: клиент показывает её только на прыгающем маунте и сам считает заполнение от удержания пробела.',
        en: 'The horse jump bar cannot be driven by a server: the client only draws it on a jumpable mount and computes the fill itself from how long jump is held.',
      },
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
        ru: 'x, y — доля экрана от левого верхнего угла (шаг 1/512), size — доля ширины экрана (шаг 1/64). PNG любой формы до 254×254: фреймворк сам дополняет до квадрата, добавляет прозрачную рамку в 1 пиксель и ставит в углы метку, по которой шейдер узнаёт картинку. Цвет текста вокруг на картинку не влияет.',
        en: 'x, y are screen fractions from the top-left corner (1/512 steps), size is a fraction of the screen width (1/64 steps). Any PNG shape up to 254×254: the framework pads it to a square, adds a transparent 1-pixel border and stamps a marker in the corners that the shader recognises the image by. Text colours around it do not matter.',
      },
    },
    {
      title: { ru: 'Анимация на экране', en: 'Screen animation' },
      code: `object Intro : EffectiveScreenAnimation() {
    override fun getNamespacedData() = MyPlugin.instance to "intro"
    override fun getFramesPath() = "textures/screen/intro/"
    override fun getFramesPerSecond() = 60
    override fun getSize() = 1f
    fun init() {}
}

Intro.show(player, Playback.ONCE)
Intro.hide(player)`,
      note: {
        ru: 'Кадры — PNG из папки в jar, по порядку имён (2 раньше 10), все одного размера. show отправляет все кадры один раз, дальше клиент листает их сам по своим игровым часам: ни пакета на кадр, пинг и его разброс анимацию не дёргают. Часы клиента идут с долями тика, поэтому FPS анимации может быть выше 20 (например, 60). Позиция и размер (getX, getY, getSize) зашиты в плитки, а цвет несёт тик старта и режим: getComponent(player, playback) и show запускают анимацию с первого кадра в момент получения — LOOP по кругу, ONCE один раз и стоп на последнем кадре, PAUSED стоит на первом, ENDED — на последнем, REVERSE один раз задом наперёд и стоп на первом. Компонент нулевой ширины можно ставить заголовком диалога — тогда анимация рисуется поверх размытого фона. До 4095 кадров.',
        en: 'Frames are the PNG files of a jar folder in name order (2 before 10), all the same size. show sends every frame once, then the client flips them by its own game clock: no packet per frame, so ping and jitter cannot make it stutter. The client clock includes tick fractions, so the frame rate may exceed 20 (e.g. 60). Position and size (getX, getY, getSize) are baked into the tiles, and the colour carries the start tick and the mode: getComponent(player, playback) and show start the animation from the first frame when it arrives — LOOP over and over, ONCE once and stop on the last frame, PAUSED stays on the first, ENDED on the last, REVERSE once backwards and stop on the first. The zero-width component can be a dialog title, then the animation is drawn above the blurred background. Up to 4095 frames.',
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
      'Каждая картинка и экранный текст показываются своим боссбаром с невидимой полоской, поэтому сколько угодно элементов живут вместе, а title остаётся свободным. Требует включённого ресурспака фреймворка; без пака игрок увидит зелёную полоску с квадратиком.',
      'Невидимым пак делает зелёный цвет боссбара — зелёные боссбары других плагинов тоже пропадут. Клиент рисует боссбары до трети высоты экрана: влезает примерно 6 элементов при масштабе интерфейса 3 и 9 при масштабе 2, а настоящие боссбары (дракон, визер, рейд) встают ниже наших.',
      'Фон диалога задаётся в EffectiveDialog.hasBackground: false — без размытия и затемнения, картинки и анимации из HUD видны под окном чётко. Пак фреймворка делает затемнение (inworld_menu_background.png) прозрачным и подменяет размытие меню (post_effect/blur.json) своим шейдером, который размывает и затемняет как ванилла, если в левом верхнем пикселе экрана нет метки диалога без фона. При размытии фона меню 0 в настройках клиента у игрока не размывается и не затемняется ни одно внутриигровое меню. Ещё пак делает прозрачной кнопку-предупреждение «пользовательский экран» справа от заголовка диалога (gui/sprites/dialog/warning_button*, спрайт — вся кнопка). Клиент добавляет её всегда, поэтому она только скрыта: на её месте всё равно всплывает подсказка и по клику открывается экран-предупреждение.',
      'EffectiveScreenText находит строку по тому, как клиент раскладывает имя боссбара (по центру экрана, обычный размер текста), поэтому getComponent() текста работает только как имя боссбара. scale считается от обычного текста интерфейса: 1 — как в чате, 4 (по умолчанию) — как title, до 16.',
      'Шейдерпаки (Iris, OptiFine) подменяют core-шейдеры — у таких игроков картинка/текст встанут не туда. Затемнение и тряска работают всегда.',
      'EffectiveScreenText пишет своим шрифтом — копией ванильной латиницы, кириллицы и диакритики с меткой в текстурах. Обычный текст шейдер не трогает, какого бы цвета он ни был. Символов, которых нет в этих ванильных битмапах (CJK и прочий unifont), нет и в экранном шрифте.',
      'Тряска заканчивается компенсирующим поворотом — камера возвращается туда, где была.',
    ],
    en: [
      'EffectiveScreenImage and EffectiveScreenText are experimental: the core-shader technique, API and behaviour may change. Fade and shake are stable.',
      'Every image and screen text is its own boss bar with an invisible bar, so any number of elements stay together and the title slot stays free. Needs the framework resource pack enabled; without it the player sees a green bar with a box.',
      'The pack makes the green boss bar colour invisible — other plugins\' green boss bars vanish too. The client draws boss bars down to a third of the screen height: about 6 elements fit at GUI scale 3 and 9 at scale 2, and real boss bars (dragon, wither, raid) go below ours.',
      'A dialog's background is set by EffectiveDialog.hasBackground: false drops the blur and darkening, so HUD images and animations stay sharp under the window. The framework pack makes the darkening (inworld_menu_background.png) transparent and replaces the menu blur (post_effect/blur.json) with its own shader, which blurs and darkens like vanilla unless the top-left screen pixel holds the flag of a dialog without background. With the client's menu background blur at 0 no in-game menu is blurred or darkened for that player. The pack also makes the «custom screen» warning button right of a dialog title transparent (gui/sprites/dialog/warning_button*, the sprite is the whole button). The client always adds it, so it is only hidden: its spot still shows the tooltip and opens the client\'s warning screen on click.',
      'EffectiveScreenText finds its line from how the client lays out a boss bar name (centred, normal text size), so the text getComponent() works only as a boss bar name. scale is relative to normal GUI text: 1 is chat-sized, 4 (the default) title-sized, up to 16.',
      'Shader packs (Iris, OptiFine) replace core shaders — for those players the image/text will land in the wrong place. Fade and shake always work.',
      'EffectiveScreenText writes in its own font — a copy of the vanilla Latin, Cyrillic and accented glyphs with a marker in the textures. The shader never touches ordinary text, whatever its colour. Characters missing from those vanilla bitmaps (CJK and other unifont scripts) are missing from the screen font too.',
      'Shake ends with a compensating rotation — the camera returns to where it was.',
    ],
  },
}
