import * as React from "react"
import { Upload, Trash, Image, FileText, GripHorizontal } from "lucide-react"
import { arrayMove, SortableContext } from "@dnd-kit/sortable"
import { Button } from "@@/button"
import { useDropzone } from "react-dropzone"
import { cn } from "@/components/ui/utils"
import truncate from "truncate"
import SortableList from "@/components/react/sortable_list"

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
    <div
      className={cn(
        className,
        "cursor-default flex justify-between items-center",
        "flex-row w-full h-16 mt-2 px-4",
        "border-solid border-[1px] border-gray-200 rounded-lg",
      )}
      {...props}
    >
      <div className="flex items-center flex-row gap-4 h-full">
        {file.type === "application/pdf" ? (
          <FileText className="text-rose-700 w-6 h-6" />
        ) : (
          <Image className="text-rose-700 w-6 h-6" />
        )}
        <div className="flex flex-col gap-0">
          <div className="text-[0.85rem] font-medium leading-snug">
            {truncate(file.name.split(".").slice(0, -1).join("."), 30)}
          </div>
          <div className="text-[0.7rem] text-gray-500 leading-tight">
            .{file.name.split(".").pop()} •{" "}
            {(file.size / (1024 * 1024)).toFixed(2)} MB
          </div>
        </div>
      </div>
      <div className={cn("flex gap-2")}>
        {children}

        <Button
          variant="outline"
          size="icon"
          className="select-none cursor-pointer"
          onClick={onDeleteFile}
        >
          <Trash className="w-4 h-4" />
        </Button>
      </div>
    </div>
  )
}

export const Dropzone = React.forwardRef(
  (
    {
      containerClassName,
      dropZoneClassName,
      children,
      showFilesList = true,
      showErrorMessage = true,
      ...props
    },
    ref,
  ) => {
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

    console.debug(props.filetypes)
    const dropzone = useDropzone({
      ...props,
      accept: accept,
      onDrop(acceptedFiles, fileRejections, event) {
        if (props.onDrop) props.onDrop(acceptedFiles, fileRejections, event)
        else {
          setFilesUploaded((_filesUploaded) => [
            ..._filesUploaded,
            ...acceptedFiles,
          ])

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
          <input ref={ref} {...dropzone.getInputProps()} id={props.id} />
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
          <div
            className={`flex flex-col gap-2 w-full h-fit mt-2 ${filesUploaded.length > 0 ? "pb-2" : ""}`}
          >
            <div className="w-full">
              <SortableList
                onDragEnd={handleDragEnd}
                items={filesUploaded.map((file) => file.name)}
              >
                {filesUploaded.map((fileUploaded, index) => (
                  <React.Fragment key={fileUploaded.name}>
                    {props.sortable ? (
                      <SortableList.Draggable id={fileUploaded.name}>
                        <Item
                          file={fileUploaded}
                          index={index}
                          id={fileUploaded.name}
                          onDeleteFile={() => deleteUploadedFile(index)}
                        >
                          <SortableList.DragHandle id={fileUploaded.name} />
                        </Item>
                      </SortableList.Draggable>
                    ) : (
                      <Item
                        file={fileUploaded}
                        index={index}
                        id={fileUploaded.name}
                        onDeleteFile={() => deleteUploadedFile(index)}
                      ></Item>
                    )}
                  </React.Fragment>
                ))}
              </SortableList>
            </div>
          </div>
        )}
      </div>
    )
  },
)
