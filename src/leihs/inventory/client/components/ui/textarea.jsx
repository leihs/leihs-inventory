import * as React from "react"
import { cn } from "@/components/ui/utils"

const Textarea = React.forwardRef(
  (
    { className, autoscale = true, resize = true, onBlur, onFocus, ...props },
    ref,
  ) => {
    const [isFocused, setIsFocused] = React.useState(false)
    const textareaRef = React.useRef(null)

    React.useImperativeHandle(ref, () => textareaRef.current)

    const adjustHeight = () => {
      const textarea = textareaRef.current
      if (textarea) {
        if (isFocused) {
          textarea.style.height = "auto"
          textarea.style.height = `${textarea.scrollHeight}px`
        } else {
          textarea.style.height = "2.5rem" // Set to 1 line height when not focused
        }
      }
    }

    React.useEffect(() => {
      adjustHeight()
    }, [props.value, isFocused])

    const handleBlur = (e) => {
      autoscale && setIsFocused(false)
      onBlur && onBlur(e)
    }

    const handleFocus = (e) => {
      autoscale && setIsFocused(true)
      onFocus && onFocus(e)
    }

    return (
      <textarea
        ref={textareaRef}
        style={{
          height: autoscale ? textareaRef.current?.scrollHeight : undefined,
        }}
        onFocus={handleFocus}
        onBlur={handleBlur}
        className={cn(
          "flex min-h-[60px] w-full rounded-md border border-input bg-transparent px-3 py-2 text-base shadow-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50 md:text-sm",
          autoscale && "overflow-hidden transition-all duration-200",
          !props.resize && "resize-none",
          className,
        )}
        {...props}
      />
    )
  },
)
Textarea.displayName = "Textarea"

export { Textarea }

// import * as React from "react"
//
// import { cn } from "@/components/ui/utils"
//
// const Textarea = React.forwardRef(({ className, ...props }, ref) => {
//   return (
//     <textarea
//       className={cn(
//         "flex min-h-[60px] w-full rounded-md border border-input bg-transparent px-3 py-2 text-base shadow-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50 md:text-sm",
//         className,
//       )}
//       ref={ref}
//       {...props}
//     />
//   )
// })
// Textarea.displayName = "Textarea"
//
// export { Textarea }
