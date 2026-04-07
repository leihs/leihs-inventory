"use client"

import * as React from "react"
import { cn } from "@/components/ui/utils"
import { Checkbox } from "@/components/ui/checkbox"

const CheckboxGroup = React.forwardRef(
  (
    { className, value = [], onValueChange, disabled, children, ...props },
    ref,
  ) => {
    const handleToggle = (itemValue) => {
      const currentValue = Array.isArray(value) ? value : []
      const newValue = currentValue.includes(itemValue)
        ? currentValue.filter((v) => v !== itemValue)
        : [...currentValue, itemValue]
      onValueChange?.(newValue)
    }

    return (
      <div
        ref={ref}
        className={cn("flex flex-row gap-4 flex-wrap", className)}
        {...props}
      >
        {React.Children.map(children, (child) => {
          if (React.isValidElement(child) && child.props.value !== undefined) {
            return React.cloneElement(child, {
              checked: value.includes(child.props.value),
              onCheckedChange: () => handleToggle(child.props.value),
              disabled: disabled || child.props.disabled,
            })
          }
          return child
        })}
      </div>
    )
  },
)
CheckboxGroup.displayName = "CheckboxGroup"

const CheckboxGroupItem = React.forwardRef(
  ({ className, value, label, id, disabled, ...props }, ref) => {
    const itemId = id || `checkbox-group-item-${value}`

    return (
      <div className={cn("flex items-center space-x-2", className)}>
        <Checkbox
          ref={ref}
          id={itemId}
          value={value}
          disabled={disabled}
          {...props}
        />
        <label
          htmlFor={itemId}
          className="text-sm font-normal cursor-pointer select-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70"
        >
          {label}
        </label>
      </div>
    )
  },
)
CheckboxGroupItem.displayName = "CheckboxGroupItem"

export { CheckboxGroup, CheckboxGroupItem }
