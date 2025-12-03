import * as React from "react"
import { NODE_ENV, INVENTORY_ERROR_FRIENDLY_MESSAGE } from "@/env.js"

class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props)
    this.state = { hasError: false, error: null, errorInfo: null }
  }
  static getDerivedStateFromError(error) {
    return { hasError: true, error }
  }
  componentDidCatch(error, errorInfo) {
    this.setState({ errorInfo })
    console.error("Inventory UI error:", error, errorInfo)
  }
  linkify(text) {
    const parts = []
    const regex =
      /(https?:\/\/[^\s]+)|([A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,})/g
    let lastIndex = 0
    let match
    while ((match = regex.exec(text)) !== null) {
      if (match.index > lastIndex)
        parts.push(text.slice(lastIndex, match.index))
      const url = match[1]
      const email = match[2]
      if (url) {
        parts.push(
          <a
            href={url}
            className="underline"
            target="_blank"
            rel="noopener noreferrer"
          >
            {url}
          </a>,
        )
      } else if (email) {
        parts.push(
          <a href={`mailto:${email}`} className="underline">
            {email}
          </a>,
        )
      }
      lastIndex = regex.lastIndex
    }
    if (lastIndex < text.length) parts.push(text.slice(lastIndex))
    return (
      <>
        {parts.map((p, i) =>
          typeof p === "string" ? (
            <span key={i}>{p}</span>
          ) : (
            React.cloneElement(p, { key: i })
          ),
        )}
      </>
    )
  }
  render() {
    if (this.state.hasError) {
      const isProd = NODE_ENV === "production"
      const message =
        INVENTORY_ERROR_FRIENDLY_MESSAGE ||
        "An unexpected error occurred. Please try again later."
      if (isProd) {
        return (
          <div className="w-screen h-screen flex items-center justify-center p-6">
            <div className="max-w-xl text-center">
              <h1 className="text-2xl font-bold mb-3">Error</h1>
              <p className="text-muted-foreground">{this.linkify(message)}</p>
              <button
                className="mt-6 underline"
                onClick={() => location.reload()}
              >
                Reload
              </button>
              <div className="mt-4">
                <a href="/inventory" className="underline">
                  Back
                </a>
              </div>
            </div>
          </div>
        )
      }
      const err = this.state.error
      const stack =
        (err && err.stack) ||
        (this.state.errorInfo && this.state.errorInfo.componentStack) ||
        ""
      return (
        <div className="w-screen h-screen flex items-center justify-center p-6">
          <div className="max-w-2xl text-center">
            <h1 className="text-2xl font-bold mb-3">Error</h1>
            <p className="text-muted-foreground">{this.linkify(message)}</p>
            <p className="text-muted-foreground">
              {err ? String(err) : "Unexpected error"}
            </p>
            <pre className="text-left whitespace-pre-wrap text-sm bg-muted/40 rounded p-4 mt-4 overflow-auto max-h-[40vh]">
              {stack}
            </pre>
            <button
              className="mt-6 underline"
              onClick={() => location.reload()}
            >
              Reload
            </button>
            <div className="mt-4">
              <a href="/inventory" className="underline">
                Back
              </a>
            </div>
          </div>
        </div>
      )
    }
    return this.props.children
  }
}

export { ErrorBoundary }
