import { TextField, TextFieldProps } from "@mui/material"
import { Control, Controller } from "react-hook-form"

export type FormTextFieldProps = {
  field: string
  control: Control<any>
} & Omit<TextFieldProps, 'value' | 'onChange'>

export const FormTextField = (props: FormTextFieldProps) => {
  const { field, control, ...textFieldProps } = props

  return (
    <Controller
      control={control}
      name={field}
      rules={{
        required: textFieldProps.required ? "Required" : undefined
      }}
      render={(renderProps) => {
        return (
          <TextField
            {...textFieldProps}
            {...renderProps.field}
            error={renderProps.fieldState.invalid}
            helperText={renderProps.fieldState.error?.message}
          />
        )
      }}>
    </Controller>
  )
}
