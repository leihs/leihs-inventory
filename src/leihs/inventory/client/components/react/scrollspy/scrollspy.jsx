import { useEffect } from "react"
import { useInView } from "react-intersection-observer"
import { useScrollspy, ScrollspyProvider } from "./context"
import { Button } from "@@/button"
import { cn } from "@@/utils"

export function Scrollspy({ children, className }) {
  return (
    <ScrollspyProvider>
      <div className={className + " scroll-smooth"}>{children}</div>
    </ScrollspyProvider>
  )
}

export function ScrollspyItem({ children, className, name, id }) {
  const { ref, inView } = useInView({
    threshold: 1,
  })
  const { setCurrent, addItem } = useScrollspy()

  useEffect(() => {
    addItem({ name: name, id: id })
  }, [])

  useEffect(() => {
    if (inView) {
      setCurrent({ name: name, id: id })
    }
  }, [inView, setCurrent])

  return (
    <div id={id} ref={ref} className={cn("", className)}>
      {children}
    </div>
  )
}

export function ScrollspyMenu({ children, className, ...props }) {
  const { items, current } = useScrollspy()
  const none = items.every((item) => item.id !== current.id)

  return (
    <aside className="min-w-max w-1/5 h-max sticky top-[10vh]" {...props}>
      <nav
        className={cn(
          "flex space-x-2 lg:flex-col lg:space-x-0 lg:space-y-1",
          className,
        )}
      >
        {items.map((item, index) => (
          <Button
            key={index}
            variant={
              item.id === current.id || (none && index === 0)
                ? "secondary"
                : "ghost"
            }
            onClick={() =>
              document
                .getElementById(item.id)
                .scrollIntoView({ behavior: "smooth" })
            }
            className={cn("justify-start")}
          >
            {item.name}
          </Button>
        ))}
      </nav>
    </aside>
  )
}

export default Scrollspy
