import type { Section } from './types'
import { setup } from './setup'
import { item } from './item'
import { block } from './block'
import { entity } from './entity'
import { composite } from './composite'
import { menu } from './menu'
import { texturemenu } from './texturemenu'
import { zone } from './zone'
import { command } from './command'
import { locale } from './locale'
import { resourcepack } from './resourcepack'
import { screen } from './screen'
import { advancement } from './advancement'
import { datacontainer } from './datacontainer'
import { events } from './events'
import { utils } from './utils'

export const sections: Record<string, Section> = {
  setup, events, item, block, entity, composite, menu, texturemenu, zone, command, locale, resourcepack, screen, advancement, datacontainer, utils,
}
export type { Section, Method, Example, Text } from './types'
