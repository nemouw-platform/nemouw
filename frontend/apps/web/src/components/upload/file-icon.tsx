import { cn } from "@workspace/ui/lib/utils"
import {
  FileArchiveIcon,
  FileIcon,
  FileImageIcon,
  FileSpreadsheetIcon,
  FileTextIcon,
  type LucideIcon,
} from "lucide-react"

import { fileKind, type FileKind } from "@/components/upload/files"

const ICONS: Record<FileKind, { icon: LucideIcon; tone: string }> = {
  image: { icon: FileImageIcon, tone: "text-primary" },
  pdf: { icon: FileTextIcon, tone: "text-destructive" },
  document: { icon: FileTextIcon, tone: "text-primary" },
  spreadsheet: { icon: FileSpreadsheetIcon, tone: "text-primary" },
  archive: { icon: FileArchiveIcon, tone: "text-muted-foreground" },
  other: { icon: FileIcon, tone: "text-muted-foreground" },
}

/** A file's icon by its type (PDF, image, spreadsheet, ...), on a tile */
export function FileTypeIcon({
  mimeType,
  className,
}: {
  mimeType: string
  className?: string
}) {
  const { icon: Icon, tone } = ICONS[fileKind(mimeType)]
  return (
    <span
      className={cn(
        "flex size-10 shrink-0 items-center justify-center rounded-md bg-muted",
        className
      )}
    >
      <Icon aria-hidden className={cn("size-5", tone)} />
    </span>
  )
}
