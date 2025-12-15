# Error Boundary Testing Guide

## Overview

The React Router error boundary has been implemented and is now active. This guide will help you test it properly.

## Important: Understanding Error Boundaries

React Router's error boundaries (`errorElement`) catch:

- ✅ Errors during **component rendering**
- ✅ Errors in **loaders** (data fetching before route renders)
- ✅ Errors in **actions** (form submissions)
- ✅ **404 / routing errors**

React Router error boundaries DO NOT catch:

- ❌ Errors in **event handlers** (onClick, onChange, etc.)
- ❌ Errors in **async code** outside loaders/actions
- ❌ Errors in **setTimeout/setInterval**

This is standard React behavior - event handler errors need to be caught differently.

## How to Test the Error Boundary

### 1. Start the Development Server

```bash
npm run dev
```

### 2. Navigate to the Debug Page

Visit: `http://localhost:YOUR_PORT/inventory/debug`

You'll see three test buttons:

### Test 1: Render Error (Component Error)

Click the **"Trigger Render Error"** button. This will:

- Set state to trigger a component that throws during render
- The error boundary WILL catch this
- You should see the error page with "Failed to Load Data" message

### Test 2: Loader Error

Click the **"Test Loader Error"** link. This will:

- Navigate to `/inventory/test-error`
- The loader for this route throws an error immediately
- The error boundary WILL catch this
- You should see the error page with technical details

### Test 3: 404 Error

Click the **"Test 404 Error"** link. This will:

- Navigate to `/inventory/this-page-does-not-exist`
- React Router throws a 404 error
- The error boundary WILL catch this
- You should see "Page Not Found" error page

## What You Should See

When an error is caught, you'll see:

- An appropriate icon (🔍 for 404, ⚠️ for errors, etc.)
- A translated error title
- A description of what went wrong
- Action buttons:
  - **Go Back** (for non-404 errors)
  - **Retry** (for network/loader errors)
  - **Go to Home** (always available)
- Technical details section (collapsible, for debugging)

## Console Logging

Check the browser console for debug information:

```
[Error Boundary] Caught error: Error {message: "...", stack: "..."}
[Error Boundary] Error type: function Error() { [native code] }
[Error Boundary] Error keys: ["message", "stack", ...]
[Error Boundary] Type: loader-error Error {message: "..."}
```

## Files Modified

1. **src/leihs/inventory/client/routes/error.cljs** - New error boundary component
2. **src/leihs/inventory/client/routes.cljs** - Enabled errorElement on root route
3. **src/leihs/inventory/client/loader.cljs** - Added test loader that throws error
4. **src/leihs/inventory/client/components/error.cljs** - Updated to throw render errors
5. **src/leihs/inventory/client/routes/debug/page.cljs** - Added test UI
6. **resources/public/inventory/assets/locales/en/translation.json** - Added error translations
7. **resources/public/inventory/assets/locales/de/translation.json** - Added German translations

## Common Issues

### "The error boundary is not triggered at all"

This usually means the error is being thrown in an event handler. Remember:

- ✅ Render errors are caught
- ✅ Loader errors are caught
- ❌ onClick/onChange errors are NOT caught

To test properly, use the provided test buttons which trigger render and loader errors.

### "I see a white screen instead of the error page"

Check the browser console for:

1. JavaScript errors in the error boundary component itself
2. Missing translation keys
3. React Router configuration issues

### "The error shows but translations are missing"

Make sure the translation keys are loaded:

- `error.boundary.not_found.title`
- `error.boundary.loader_error.title`
- etc.

Check `resources/public/inventory/assets/locales/*/translation.json`

## Next Steps

Once you've verified the error boundary works:

1. **Review Loaders** - Decide which loaders should re-throw errors vs. silently fail
2. **Add More Error Elements** - Consider adding errorElement to specific child routes
3. **Error Tracking** - Integrate with Sentry or similar service for production monitoring
4. **HTTP Interceptors** - Add axios interceptors for global API error handling

## Production Considerations

Before deploying:

1. **Hide Technical Details** - Set `show-details?` based on environment:

   ```clojure
   show-details? (and (= error-type :unknown)
                      (not js/process.env.PRODUCTION))
   ```

2. **Error Reporting** - Send errors to monitoring service:

   ```clojure
   (when js/process.env.PRODUCTION
     (report-error-to-sentry error))
   ```

3. **User-Friendly Messages** - Review all error messages for clarity

4. **Test All Routes** - Manually test that each route's loader/action error handling works as expected
