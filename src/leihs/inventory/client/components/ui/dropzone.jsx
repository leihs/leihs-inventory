import * as React from "react"
import { Upload, Trash, Image, FileText } from "lucide-react"
import { arrayMove } from "@dnd-kit/sortable"
import { Button } from "@@/button"
import { Card } from "@@/card"
import { useDropzone } from "react-dropzone"
import { cn } from "@/components/ui/utils"
import truncate from "truncate"
import SortableList from "@/components/react/sortable-list"
import {
  Table,
  TableHeader,
  TableHead,
  TableRow,
  TableBody,
  TableCell,
} from "@/components/ui/table"

function Item({
  children,
  className,
  id,
  file,
  onDeleteFile,
  index,
  ...props
}) {
  return (
    <>
      <TableCell className="flex gap-4 items-center">
        {file.type === "application/pdf" ? (
          <FileText className="w-10 h-10" />
        ) : (
          <Image className="w-10 h-10" />
        )}
        <div className="flex flex-col gap-0">
          <div className="text-[0.85rem] font-medium leading-snug">
            {truncate(file.name.split(".").slice(0, -1).join("."), 30)}
          </div>
          <div className="text-[0.7rem] text-gray-500 leading-tight uppercase">
            {file.name.split(".").pop()},{" "}
            {(file.size / (1024 * 1024)).toFixed(2)} MB
          </div>
        </div>
      </TableCell>

      {children}

      <TableCell>
        <div className="flex gap-2 justify-end">
          <SortableList.DragHandle id={id} />
          <Button
            variant="outline"
            size="icon"
            className="select-none cursor-pointer"
            onClick={onDeleteFile}
          >
            <Trash className="w-4 h-4" />
          </Button>
        </div>
      </TableCell>
    </>
  )
}

export const Dropzone = React.forwardRef(
  (
    {
      containerClassName,
      dropZoneClassName,
      children,
      itemExtensions,
      showFilesList = true,
      showErrorMessage = true,
      setFilesUploadedExternal, // Add this line
      ...props
    },
    ref,
  ) => {
    const [filesUploaded, setFilesUploaded] = React.useState([])
    const [errorMessage, setErrorMessage] = React.useState()

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
        if (props.onDrop)
          props.onDrop(acceptedFiles, fileRejections, setFilesUploaded)
        else {
          setFilesUploaded((_filesUploaded) => {
            acceptedFiles.map((file) => {
              file.custom = "hello"
            })

            // console.debug("new", acceptedFiles)
            return [..._filesUploaded, ...acceptedFiles]
          })

          if (fileRejections.length > 0) {
            let _errorMessage = `Could not upload ${fileRejections[0].file.name}`
            if (fileRejections.length > 1)
              _errorMessage =
                _errorMessage +
                `, and ${fileRejections.length - 1} other files.`
            setErrorMessage(_errorMessage)
          } else {
            setErrorMessage("")
          }
        }
      },
    })

    React.useEffect(() => {
      if (setFilesUploadedExternal) {
        setFilesUploadedExternal(setFilesUploaded)
      }
    }, [setFilesUploadedExternal])

    React.useEffect(() => {
      console.debug("change", filesUploaded)
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

    const deleteUploadedFile = (index) => {
      setFilesUploaded((_uploadedFiles) => [
        ..._uploadedFiles.slice(0, index),
        ..._uploadedFiles.slice(index + 1),
      ])
    }

    return (
      <div className={cn("flex flex-col gap-2", containerClassName)}>
        <div
          {...dropzone.getRootProps()}
          className={cn(
            "flex justify-center items-center w-full h-32 border-dashed border-2 border-gray-200 rounded-lg hover:bg-accent hover:text-accent-foreground transition-all select-none cursor-pointer",
            dropZoneClassName,
          )}
        >
          <input
            ref={ref}
            {...dropzone.getInputProps()}
            id={props.id}
            onBlur={props.onBlur}
            aria-describedby={props["aria-describedby"]}
            aria-invalid={props["aria-invalid"]}
            name={props.name}
          />

          {children ? (
            children(dropzone)
          ) : dropzone.isDragAccept ? (
            <div className="text-sm font-medium">Drop your files here!</div>
          ) : (
            <div className="flex items-center flex-col gap-1.5">
              <div className="flex items-center flex-row gap-0.5 text-sm font-medium">
                <Upload className="mr-2 h-4 w-4" /> Upload files
              </div>
              {props.maxSize && (
                <div className="text-xs text-gray-400 font-medium">
                  Max. file size: {(props.maxSize / (1024 * 1024)).toFixed(2)}{" "}
                  MB
                </div>
              )}
            </div>
          )}
        </div>
        {errorMessage && (
          <span className="text-xs text-red-600 mt-3">{errorMessage}</span>
        )}
        {showFilesList && filesUploaded.length > 0 && (
          <div className="rounded-md border">
            <div className="w-full">
              <SortableList
                onDragEnd={handleDragEnd}
                items={filesUploaded.map((file) => file.name)}
              >
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Files</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {filesUploaded.map((fileUploaded, index) => (
                      <React.Fragment key={fileUploaded.name}>
                        {props.sortable ? (
                          <SortableList.Draggable
                            asChild={true}
                            id={fileUploaded.name}
                          >
                            <TableRow>
                              <Item
                                file={fileUploaded}
                                index={index}
                                id={fileUploaded.name}
                                onDeleteFile={() => deleteUploadedFile(index)}
                              >
                                {itemExtensions}
                              </Item>
                            </TableRow>
                          </SortableList.Draggable>
                        ) : (
                          <TableRow>
                            <Item
                              file={fileUploaded}
                              index={index}
                              id={fileUploaded.name}
                              onDeleteFile={() => deleteUploadedFile(index)}
                            >
                              {itemExtensions}
                            </Item>
                          </TableRow>
                        )}
                      </React.Fragment>
                    ))}
                  </TableBody>
                </Table>
              </SortableList>
            </div>
          </div>
        )}
      </div>
    )
  },
)
