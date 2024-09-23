import React, { useRef, useState } from "react"

// Create the context
const ScrollspyContext = React.createContext()

// Create a provider component
export const ScrollspyProvider = ({ children }) => {
  const [current, setCurrent] = React.useState(0)
  const [items, setItems] = useState([])

  // Function to add an item
  function addItem(item) {
    if (items.find((i) => i.id === item.id)) return

    setItems((prev) => [...prev, item])
  }

  const value = {
    current,
    setCurrent,
    items,
    addItem,
  }

  return (
    <ScrollspyContext.Provider value={value}>
      {children}
    </ScrollspyContext.Provider>
  )
}

// Create a custom hook for using the context
export const useScrollspy = () => {
  const context = React.useContext(ScrollspyContext)
  if (context === undefined) {
    throw new Error("useScrollspy must be used within a ScrollspyProvider")
  }
  return context
}
