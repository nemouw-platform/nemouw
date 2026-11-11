"use client"

import { CheckIcon, ChevronDownIcon, GlobeIcon } from "lucide-react"

import { Button } from "@workspace/ui/components/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuLinkItem,
  DropdownMenuTrigger,
} from "@workspace/ui/components/dropdown-menu"

export interface LanguageOption {
  /** Language code, used for `lang` and `hrefLang` */
  value: string
  /** The language written in itself, e.g. "Français" */
  label: string
  /** Same page in that language */
  href: string
}

interface LanguageMenuProps {
  options: LanguageOption[]
  /** The current language's value */
  value: string
  /** Accessible name of the menu, e.g. "Language" */
  label: string
  /** Text next to the globe; defaults to the current language's label */
  triggerText?: React.ReactNode
}

/**
 * Globe button that opens the list of languages. Each option is a real link,
 * so choosing one loads the page in that language.
 */
export function LanguageMenu({
  options,
  value,
  label,
  triggerText,
}: LanguageMenuProps) {
  const current = options.find((option) => option.value === value)

  return (
    <DropdownMenu>
      <DropdownMenuTrigger
        render={
          <Button
            variant="ghost"
            size="sm"
            aria-label={current ? `${label}: ${current.label}` : label}
          />
        }
      >
        <GlobeIcon data-icon="inline-start" />
        {triggerText ?? current?.label}
        <ChevronDownIcon className="opacity-60" />
      </DropdownMenuTrigger>

      <DropdownMenuContent align="end" className="w-auto min-w-40">
        {options.map((option) => {
          const selected = option.value === value
          return (
            <DropdownMenuLinkItem
              key={option.value}
              href={option.href}
              lang={option.value}
              hrefLang={option.value}
              aria-current={selected ? "true" : undefined}
            >
              <span className="flex-1">{option.label}</span>
              {selected && <CheckIcon />}
            </DropdownMenuLinkItem>
          )
        })}
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
