export default {
  nav: {
    home: 'Home',
    roadmap: 'Roadmap',
  },
  footer: {
    author: 'Author',
    license: 'License',
  },
  home: {
    title: 'Write plugins, not the same code again',
    lead: 'Once your SMP server runs ten plugins instead of one, they start repeating each other: custom items, mobs, menus, resource pack, configs, localisation. At some point you think about your own library. EffectiveSpigot is that library, already written.',
    what: 'What is inside',
    whatLead: 'Base classes and interfaces for the routine work of an SMP plugin. Extend, override a couple of methods — the framework does the rest.',
    interfaces: 'Interfaces',
    eyebrow: 'Kotlin · Paper · framework',
    ctaRoadmap: 'Where to start',
    ctaGithub: 'GitHub',
    example: 'minimal item',
    concepts: 'Three rules',
    conceptsLead: 'Everything in the framework works the same way. Get these and you get every section.',
    concept: {
      identity: {
        title: 'A key instead of checks',
        text: 'Every item, mob, block or menu declares a plugin and an id — together they form a key like myplugin:ruby. It is written to PDC on creation, so any ItemStack or Entity from the world can be asked "whose are you?" — no comparing names, lore or materials.',
        code: 'EffectiveItem.getNamespacedKeyByItem(stack) == RubyItem.getNamespacedName()',
      },
      init: {
        title: 'object is lazy — call init()',
        text: 'A Kotlin object is created on first access. Until something touches RubyItem it does not exist: not registered, not in /egive, recipes do not work. That is why each has an empty init() called from onEnable (commands — from onLoad). Registration happens in the constructor; a duplicate key throws.',
        code: 'onEnable → RubyItem.init()',
      },
      optin: {
        title: 'Behaviour on demand',
        text: 'A base class does almost nothing by itself: clicks, recipes, throwing, durability are enabled by separate calls. Each wraps an Effective* interface that can also be called directly with a vanilla object: hook a click on any ItemStack or make any pig look at the player.',
        code: 'makeWearable()  ·  EffectiveEntityLookable.doEntityLookable(pig)',
      },
    },
    steps: {
      gradle: { title: 'Apply the gradle plugin', text: 'One line in the plugins block — Kotlin, shadow, relocation and the framework dependency are already wired.' },
      extend: { title: 'Extend Effective*', text: 'An item, mob, block, menu or zone is a class with a couple of overridden methods.' },
      run: { title: 'Start the server', text: 'Registration, recipes and the resource pack with textures and fonts are built on startup.' },
    },
  },
  roadmap: {
    eyebrow: 'where to start',
    title: 'Roadmap',
    lead: 'Six stages in the order worth going through. Dashed cards build on the class above them and come after it. Each section: why, minimal example, what to override, pitfalls.',
    extends: 'after',
  },
  section: {
    why: 'Why',
    example: 'Minimal example',
    details: 'In detail',
    methods: 'What to override',
    method: 'Method',
    default: 'Default',
    desc: 'Description',
    required: 'required',
    pitfalls: 'Pitfalls',
    prev: 'Back',
    next: 'Next',
  },
  status: {
    experimental: 'experimental',
    wip: 'work in progress',
    experimentalNote: 'Experimental part: the API may change, behaviour across clients and with shader packs is not guaranteed.',
    wipNote: 'This subsystem is not finished yet: some mechanics may work partially or with bugs, the API may change.',
  },
  theme: {
    light: 'Light theme',
    dark: 'Dark theme',
  },
}
