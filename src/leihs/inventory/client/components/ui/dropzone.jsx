import * as React from "react"
import { Upload, Trash, Image, FileText } from "lucide-react"
import { arrayMove } from "@dnd-kit/sortable"
import { Button } from "@@/button"
import { useDropzone } from "react-dropzone"
import { cn } from "@/components/ui/utils"
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import SortableList from "@/components/react/sortable-list"
import {
  Table,
  TableHeader,
  TableHead,
  TableRow,
  TableBody,
  TableCell,
} from "@/components/ui/table"
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip"

function Item({ children, className, file, generatePreview = true }) {
  const [preview, setPreview] = React.useState()
  const [open, setOpen] = React.useState(false)

  React.useEffect(() => {
    if (!generatePreview) return

    if (file && file instanceof File) {
      const objectUrl = URL.createObjectURL(file)
      setPreview(objectUrl)
      return () => URL.revokeObjectURL(objectUrl)
    } else {
      console.error("Invalid file object:", file)
    }
  }, [file])

  const handleOpen = () => {
    setOpen((prev) => !prev)
  }

  return (
    <>
      <TableCell className="flex gap-4 items-center">
        {generatePreview === false || file.type === "application/pdf" ? (
          <FileText className="w-10 h-10" />
        ) : (
          <>
            {file.type.startsWith("image/") ? (
              <TooltipProvider>
                <Tooltip>
                  <TooltipTrigger className="cursor-pointer" asChild>
                    <img
                      src={preview}
                      className="w-10 h-10 rounded object-cover"
                      onClick={handleOpen}
                    />
                  </TooltipTrigger>
                  <TooltipContent>
                    <img src={preview} className="w-96 h-auto rounded" />
                  </TooltipContent>
                </Tooltip>
              </TooltipProvider>
            ) : (
              <FileText className="w-10 h-10" />
            )}
          </>
        )}
        <div className="flex flex-col gap-0">
          <div className="text-[0.85rem] font-medium leading-snug">
            {file.name}
          </div>
          <div className="text-[0.7rem] text-gray-500 leading-tight uppercase">
            {file.name.split(".").pop()}, {(file.size / 1024).toFixed(2)} kB
          </div>
        </div>
        <Dialog open={open} onOpenChange={handleOpen}>
          <DialogContent className="max-w-fit">
            <DialogHeader>
              <DialogTitle>{file.name}</DialogTitle>
            </DialogHeader>
            <img src={preview} className="w-fit h-auto rounded" />
          </DialogContent>
        </Dialog>
      </TableCell>

      {children}
    </>
  )
}

const DropzoneArea = React.forwardRef(({ className, ...props }, ref) => {
  const filetypes = {
    jpeg: { "image/jpeg": [".jpg", ".jpeg"] },
    png: { "image/png": [".png"] },
    pdf: { "application/pdf": [".pdf"] },
  }

  const accept =
    props.filetypes && props.filetypes.includes(",")
      ? props.filetypes
          // create array
          .split(",")
          //map filetypes from splitted filetypes
          .map((type) => filetypes[type])
          // reduce array of filetypes to a single object
          .reduce((acc, cur) => ({ ...acc, ...cur }), {})
      : // when filteypes is single type without comma
        props.filetypes
        ? filetypes[props.filetypes]
        : []

  const dropzone = useDropzone({
    ...props,
    accept: accept,
    onDrop(acceptedFiles, fileRejections, event) {
      props.onDrop && props.onDrop(acceptedFiles, fileRejections, event)
    },
  })

  return (
    <div
      {...dropzone.getRootProps()}
      className={cn(
        "flex shadow-sm justify-center items-center w-full h-32 border-dashed border-2 border-gray-200 rounded-lg hover:bg-accent hover:text-accent-foreground transition-all select-none cursor-pointer",
        className,
      )}
    >
      <input
        ref={ref}
        {...dropzone.getInputProps()}
        id={props.id}
        multiple={props.multiple}
        onBlur={props.onBlur}
        aria-describedby={props["aria-describedby"]}
        aria-invalid={props["aria-invalid"]}
        name={props.name}
      />

      {dropzone.isDragAccept ? (
        <div className="text-sm font-medium shadow-sm">
          Drop your files here!
        </div>
      ) : (
        <div className="flex items-center flex-col gap-1.5">
          <div className="flex items-center flex-row gap-0.5 text-sm font-medium">
            <Upload className="mr-2 h-4 w-4" />
          </div>
          {props.maxSize && (
            <div className="text-xs text-gray-400 font-medium">
              Max. file size: {(props.maxSize / (1024 * 1024)).toFixed(2)} MB
            </div>
          )}
        </div>
      )}
    </div>
  )
})

function DropzoneFiles({ children }) {
  return (
    <div className="rounded-md border">
      <div className="w-full">{children}</div>
    </div>
  )
}

function ErrorMessages({ rejections }) {
  return (
    <>
      {rejections.map((rejection) => (
        <div
          key={rejection.file.name}
          className="text-[0.8rem] font-medium text-destructive mt-3"
        >
          <div>{rejection.file.name}</div>
          <div>
            {rejection.errors.map((err) => (
              <div>{err.message}</div>
            ))}
          </div>
        </div>
      ))}
    </>
  )
}

const Dropzone = React.forwardRef(
  (
    {
      containerClassName,
      dropZoneClassName,
      children,
      itemExtensions,
      showFilesList = true,
      showErrorMessage = true,
      ...props
    },
    ref,
  ) => {
    const [filesUploaded, setFilesUploaded] = React.useState([])
    const [errorMessage, setErrorMessage] = React.useState()

    React.useEffect(() => {
      if (props.onChange) {
        props.onChange(filesUploaded)
      }
    }, [filesUploaded])

    const handleDragEnd = (event) => {
      const { active, over } = event

      if (active.id !== over.id) {
        setFilesUploaded((items) => {
          const oldIndex = items.findIndex((item) => item.name === active.id)
          const newIndex = items.findIndex((item) => item.name === over.id)

          return arrayMove(items, oldIndex, newIndex)
        })
      }
    }

    function handleDrop(acceptedFiles, fileRejections, event) {
      setFilesUploaded((_filesUploaded) => [
        ..._filesUploaded,
        ...acceptedFiles,
      ])

      if (fileRejections.length > 0) {
        const { rejections } = fileRejections
        setErrorMessage(fileRejections)
      } else {
        setErrorMessage(null)
      }
    }

    const deleteUploadedFile = (index) => {
      setFilesUploaded((_uploadedFiles) => [
        ..._uploadedFiles.slice(0, index),
        ..._uploadedFiles.slice(index + 1),
      ])
    }

    return (
      <div className={cn("flex flex-col gap-2", containerClassName)}>
        {children ? (
          children
        ) : (
          <>
            <DropzoneArea
              ref={ref}
              className={dropZoneClassName}
              onDrop={(acceptedFiles, fileRejections, event) =>
                handleDrop(acceptedFiles, fileRejections, event)
              }
              {...props}
            />
            {errorMessage && errorMessage.length > 0 && (
              <>
                <ErrorMessages rejections={errorMessage} />
              </>
            )}

            {showFilesList && filesUploaded.length > 0 && (
              <DropzoneFiles>
                <SortableList
                  onDragEnd={handleDragEnd}
                  items={filesUploaded.map((file) => file.name)}
                >
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>Bezeichnung</TableHead>
                        <TableHead></TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {filesUploaded.map((fileUploaded, index) => (
                        <React.Fragment key={fileUploaded.name}>
                          <SortableList.Draggable
                            asChild={true}
                            id={fileUploaded.name}
                          >
                            <TableRow>
                              <Item file={fileUploaded}>
                                <TableCell>
                                  <div className="flex gap-2 justify-end">
                                    {props.sortable && (
                                      <SortableList.DragHandle
                                        id={fileUploaded.name}
                                      />
                                    )}
                                    <Button
                                      variant="outline"
                                      size="icon"
                                      className="select-none cursor-pointer"
                                      onClick={() => deleteUploadedFile(index)}
                                    >
                                      <Trash className="w-4 h-4" />
                                    </Button>
                                  </div>
                                </TableCell>
                              </Item>
                            </TableRow>
                          </SortableList.Draggable>
                        </React.Fragment>
                      ))}
                    </TableBody>
                  </Table>
                </SortableList>
              </DropzoneFiles>
            )}
          </>
        )}
      </div>
    )
  },
)

export { Item, DropzoneArea, DropzoneFiles, Dropzone, ErrorMessages }
