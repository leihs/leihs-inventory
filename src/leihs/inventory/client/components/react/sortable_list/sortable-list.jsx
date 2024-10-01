import * as React from "react"
import { GripHorizontal } from "lucide-react"
import { DndContext, closestCenter } from "@dnd-kit/core"
import { SortableContext } from "@dnd-kit/sortable"
import { Button } from "@@/button"
import { useSortable, rectSortingStrategy } from "@dnd-kit/sortable"
import { CSS } from "@dnd-kit/utilities"
import { cn } from "@/components/ui/utils"
import { Slot } from "@radix-ui/react-slot"

function SortableList({ children, items, onDragEnd }) {
  return (
    <DndContext collisionDetection={closestCenter} onDragEnd={onDragEnd}>
      <SortableContext items={items} strategy={rectSortingStrategy}>
        {children}
      </SortableContext>
    </DndContext>
  )
}

function Draggable({ children, className, id, ...props }) {
  const { transition, transform, setNodeRef } = useSortable({ id })

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
  }
  return (
    <div style={style} {...props} ref={setNodeRef}>
      {children}
    </div>
  )
}

const DragHandle = React.forwardRef(
  ({ children, className, id, asChild = false, ...props }, ref) => {
    const Comp = asChild ? Slot : Button
    const { listeners } = useSortable({ id })
    return (
      <Comp
        ref={ref}
        variant="outline"
        size="icon"
        {...props}
        {...listeners}
        className="select-none cursor-grab"
      >
        <GripHorizontal className="w-4 h-4" />
        {children}
      </Comp>
    )
  },
)

SortableList.displayName = "Sortable List"
SortableList.Draggable = Draggable
SortableList.DragHandle = DragHandle

export { SortableList, Draggable, DragHandle }
