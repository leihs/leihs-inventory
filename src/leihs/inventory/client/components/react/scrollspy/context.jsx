import React from "react"

// Create the context
const ScrollspyContext = React.createContext()

// Create a provider component
export const ScrollspyProvider = ({ children }) => {
  const [current, setCurrent] = React.useState(0)
  const [items, setItems] = React.useState([])

  // Function to add an item
  function addItem(item) {
    setItems((prev) => {
      if (!Array.isArray(prev)) return []

      const index = prev.findIndex((i) => i.id === item.id)
      if (index === -1) {
        return [...prev, item]
      }

      if (prev[index].name === item.name) {
        return [...prev]
      }

      return prev.map((i, idx) => (idx === index ? item : i))
    })
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
