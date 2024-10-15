import * as React from "react"
import { GripHorizontal } from "lucide-react"
import { DndContext, closestCenter } from "@dnd-kit/core"
import { Button } from "@@/button"
import {
  SortableContext,
  arrayMove,
  useSortable,
  rectSortingStrategy,
} from "@dnd-kit/sortable"
import { CSS } from "@dnd-kit/utilities"
import { cn } from "@/components/ui/utils"
import { Slot } from "@radix-ui/react-slot"

function SortableList({ children, items, onDragEnd, setItems = false }) {
  const handleDragEnd = (event) => {
    const { active, over } = event

    if (active.id !== over.id) {
      setItems((items) => {
        const oldIndex = items.findIndex((item) => item.id === active.id)
        const newIndex = items.findIndex((item) => item.id === over.id)

        return arrayMove(items, oldIndex, newIndex)
      })
    }
  }

  return (
    <DndContext
      collisionDetection={closestCenter}
      onDragEnd={setItems ? handleDragEnd : onDragEnd}
    >
      <SortableContext items={items} strategy={rectSortingStrategy}>
        {children}
      </SortableContext>
    </DndContext>
  )
}

function Draggable({ children, className, id, asChild = false, ...props }) {
  const { transition, transform, setNodeRef } = useSortable({ id })
  const Comp = asChild ? Slot : "div"

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
  }
  return (
    <Comp style={style} {...props} ref={setNodeRef}>
      {children}
    </Comp>
  )
}

const DragHandle = React.forwardRef(
  ({ children, className, id, asChild = false, ...props }, ref) => {
    const Comp = asChild ? Slot : Button
    const { listeners } = useSortable({ id })
    return (
      <Comp
        ref={ref}
        type="button"
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
