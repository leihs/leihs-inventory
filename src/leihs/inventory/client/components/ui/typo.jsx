import * as React from "react"
import { Slot } from "@radix-ui/react-slot"
import { cva } from "class-variance-authority"

import { cn } from "@/components/ui/utils"

const typoVariants = cva("", {
  variants: {
    variant: {
      h1: "scroll-m-20 text-3xl font-bold",
      h2: "scroll-m-20 text-2xl font-semibold",
      h3: "scroll-m-20 text-xl font-medium",
      p: "leading-7",
      link: "text-primary underline underline-offset-4 hover:text-primary/80",
      caption: "text-xs text-muted-foreground",
      label:
        "text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70",
      code: "relative rounded bg-muted px-[0.3rem] py-[0.2rem] font-mono text-sm",
      description: "text-sm text-muted-foreground",
    },
  },
  defaultVariants: {
    variant: "p",
  },
})

const variantElementMap = {
  h1: "h1",
  h2: "h2",
  h3: "h3",
  p: "p",
  link: "span",
  caption: "span",
  label: "span",
  code: "code",
  description: "p",
}

const Typo = React.forwardRef(
  ({ className, variant = "p", asChild = false, ...props }, ref) => {
    const Comp = asChild ? Slot : variantElementMap[variant]
    return (
      <Comp
        className={cn(typoVariants({ variant, className }))}
        ref={ref}
        {...props}
      />
    )
  },
)
Typo.displayName = "Typo"

export { Typo, typoVariants }
