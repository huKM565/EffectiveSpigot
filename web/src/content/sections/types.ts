export interface Text {
  ru: string
  en: string
}

export interface Example {
  title: Text
  code: string
  note?: Text
}

export interface Method {
  name: string
  required: boolean
  default?: string
  desc: Text
}

export interface Section {
  id: string
  why: { ru: string[]; en: string[] }
  examples: Example[]
  behaviours?: Example[]
  behavioursTitle?: Text
  behavioursLead?: Text
  methods: Method[]
  pitfalls: { ru: string[]; en: string[] }
}
