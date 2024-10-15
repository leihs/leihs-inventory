import * as React from "react"

import { cn } from "@/components/ui/utils"

const Textarea = React.forwardRef(
  (
    { className, autoscale = false, resize = true, onBlur, onFocus, ...props },
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

    function handleBlur(e) {
      autoscale && setIsFocused(false)
      onBlur && onBlur(e)
    }

    function handleFocus(e) {
      autoscale && setIsFocused(true)
      onFocus && onFocus(e)
    }

    return (
      <textarea
        ref={textareaRef}
        style={{
          height: autoscale ? textareaRef.current?.scrollHeight : undefined,
        }}
        onFocus={(e) => handleFocus(e)}
        onBlur={(e) => handleBlur(e)}
        className={cn(
          "flex min-h-[80px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50",
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
