"use client"

import {
  Controller,
  type Control,
  type ControllerRenderProps,
  type FieldValues,
  type Path,
} from "react-hook-form"

import {
  FormField,
  type ControlProps,
  type FieldOwnProps,
} from "@/components/form/form-field"

interface FormControllerFieldProps<
  TValues extends FieldValues,
  TName extends Path<TValues>,
> extends Omit<FieldOwnProps, "id"> {
  name: TName
  control: Control<TValues>
  /** Defaults to the field's name */
  id?: string
  /** The control, given its aria props plus react-hook-form's value, onChange, onBlur and ref */
  render: (
    props: ControlProps & ControllerRenderProps<TValues, TName>
  ) => React.ReactElement
}

/**
 * FormField for controls that are not native inputs (a select, a phone
 * field): wires react-hook-form's Controller and the field's aria props.
 */
export function FormControllerField<
  TValues extends FieldValues,
  TName extends Path<TValues>,
>({
  name,
  control,
  id,
  render,
  ...field
}: FormControllerFieldProps<TValues, TName>) {
  return (
    <FormField id={id ?? name} {...field}>
      {(controlProps) => (
        <Controller
          name={name}
          control={control}
          render={({ field: controller }) =>
            render({ ...controlProps, ...controller })
          }
        />
      )}
    </FormField>
  )
}
