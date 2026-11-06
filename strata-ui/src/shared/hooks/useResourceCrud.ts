// SPDX-License-Identifier: Apache-2.0
import { useState, type Dispatch, type SetStateAction } from "react";
import type { UseMutationResult } from "@tanstack/react-query";
import { useMutationWithToast } from "@/shared/hooks/useMutationWithToast";

/**
 * Options for {@link useResourceCrud}.
 *
 * The hook owns the three mutations (create / update / remove), the derived
 * `busy` flag, and the dialog + delete-confirmation state (`open`, `edit`,
 * `deleting`, `form`). Each page keeps its own form *fields* in JSX; the hook
 * only resets / populates the form via `initForm` and `toEditForm`.
 */
export interface UseResourceCrudOptions<
  TDto extends { id: string },
  TCreatePayload,
  TUpdatePayload,
  TForm,
> {
  /** Create request: `(payload) => Promise<unknown>`. */
  create: (payload: TCreatePayload) => Promise<unknown>;
  /** Update request: `(id, payload) => Promise<unknown>`. Omit for create-only pages. */
  update?: (id: string, payload: TUpdatePayload) => Promise<unknown>;
  /** Remove request: `(id) => Promise<unknown>`. */
  remove: (id: string) => Promise<unknown>;
  /** Called after a successful create / update / remove to refresh the list. */
  reload: () => void;
  /** Build a fresh empty form (called on `openCreate` and on first mount). */
  initForm: () => TForm;
  /** Map a DTO to its edit form (called on `openEdit`). */
  toEditForm: (dto: TDto) => TForm;
  /** Derive the create payload from the form. Defaults to identity (form is the payload). */
  toCreatePayload?: (form: TForm) => TCreatePayload;
  /** Derive the update payload from the form. Defaults to `toCreatePayload` / identity. */
  toUpdatePayload?: (form: TForm) => TUpdatePayload;
  /** Override toast message keys per operation. */
  messages?: {
    create?: { success?: string; failure?: string };
    update?: { success?: string; failure?: string };
    remove?: { success?: string; failure?: string };
  };
  /** Return false to abort `handleSave` (e.g. missing required context). Defaults to true. */
  canSave?: () => boolean;
  /** Side-effect run after a successful delete (e.g. clear a selected row). */
  onDeleted?: (dto: TDto) => void;
}

/**
 * Return value of {@link useResourceCrud}.
 */
export interface UseResourceCrudResult<
  TDto extends { id: string },
  TForm,
  TCreatePayload,
  TUpdatePayload,
> {
  createMutation: UseMutationResult<unknown, Error, TCreatePayload, unknown>;
  updateMutation: UseMutationResult<
    unknown,
    Error,
    { id: string; payload: TUpdatePayload },
    unknown
  >;
  deleteMutation: UseMutationResult<unknown, Error, string, unknown>;
  /** True while any of the three mutations is pending. */
  busy: boolean;
  /** Whether the create/edit dialog is open. */
  open: boolean;
  /** The DTO being edited, or null when creating. */
  edit: TDto | null;
  /** The DTO pending delete confirmation, or null. */
  deleting: TDto | null;
  /** Current form state (fields are edited by the page JSX). */
  form: TForm;
  setForm: Dispatch<SetStateAction<TForm>>;
  /** Reset the form via `initForm` and open the dialog for a new record. */
  openCreate: () => void;
  /** Populate the form via `toEditForm(dto)` and open the dialog. */
  openEdit: (dto: TDto) => void;
  /** Close the create/edit dialog. */
  closeDialog: () => void;
  /** Run create or update (based on `edit`), then close the dialog. */
  handleSave: () => Promise<void>;
  /** Open the delete confirmation for `dto`. */
  confirmDelete: (dto: TDto) => void;
  /** Dismiss the delete confirmation without deleting. */
  closeDelete: () => void;
  /** Perform the pending delete (used as the ConfirmDialog `onConfirm`). */
  runDelete: () => Promise<void>;
}

/**
 * Shared CRUD state machine for resource list pages.
 *
 * Replaces the ~40 lines of boilerplate repeated across list pages: three
 * `useMutationWithToast` calls, the `busy` aggregation, the `open` / `edit` /
 * `deleting` / `form` state, and the `openCreate` / `openEdit` / `handleSave`
 * / delete-confirmation handlers.
 *
 * Behavior notes:
 * - `reload` is invoked after every successful mutation (mirrors the previous
 *   `onSuccess: () => reload()`; pages that used `invalidateKeys` on the same
 *   query key are equivalent).
 * - Success / failure toasts match the previous defaults (`toast.saved` /
 *   `toast.saveFailed` for create & update, `toast.deleted` /
 *   `toast.deleteFailed` for remove), overridable via `messages`.
 * - Extra page-level mutations (e.g. a status toggle or batch delete) are not
 *   covered by `busy`; the page ORs their `isPending` into `busy` itself.
 */
export function useResourceCrud<TDto extends { id: string }, TCreatePayload, TUpdatePayload, TForm>(
  options: UseResourceCrudOptions<TDto, TCreatePayload, TUpdatePayload, TForm>,
): UseResourceCrudResult<TDto, TForm, TCreatePayload, TUpdatePayload> {
  const {
    create,
    update,
    remove,
    reload,
    initForm,
    toEditForm,
    toCreatePayload,
    toUpdatePayload,
    messages,
    canSave,
    onDeleted,
  } = options;

  const [open, setOpen] = useState(false);
  const [edit, setEdit] = useState<TDto | null>(null);
  const [deleting, setDeleting] = useState<TDto | null>(null);
  const [form, setForm] = useState<TForm>(() => initForm());

  const toCreate = toCreatePayload ?? ((f: TForm) => f as unknown as TCreatePayload);
  const toUpdate = toUpdatePayload ?? ((f: TForm) => f as unknown as TUpdatePayload);

  const createMutation = useMutationWithToast(
    (payload: TCreatePayload) => create(payload),
    {
      success: messages?.create?.success ?? "toast.saved",
      failure: messages?.create?.failure ?? "toast.saveFailed",
    },
    { onSuccess: () => reload() },
  );

  const updateMutation = useMutationWithToast(
    ({ id, payload }: { id: string; payload: TUpdatePayload }) =>
      update ? update(id, payload) : Promise.resolve(null),
    {
      success: messages?.update?.success ?? "toast.saved",
      failure: messages?.update?.failure ?? "toast.saveFailed",
    },
    { onSuccess: () => reload() },
  );

  const deleteMutation = useMutationWithToast(
    (id: string) => remove(id),
    {
      success: messages?.remove?.success ?? "toast.deleted",
      failure: messages?.remove?.failure ?? "toast.deleteFailed",
    },
    { onSuccess: () => reload() },
  );

  const busy = createMutation.isPending || updateMutation.isPending || deleteMutation.isPending;

  const closeDialog = () => setOpen(false);

  const openCreate = () => {
    setEdit(null);
    setForm(initForm());
    setOpen(true);
  };

  const openEdit = (dto: TDto) => {
    setEdit(dto);
    setForm(toEditForm(dto));
    setOpen(true);
  };

  const handleSave = async () => {
    if (canSave && !canSave()) return;
    try {
      if (edit && update) {
        await updateMutation.mutateAsync({ id: edit.id, payload: toUpdate(form) });
      } else {
        await createMutation.mutateAsync(toCreate(form));
      }
      closeDialog();
    } catch {
      // toast already shown
    }
  };

  const confirmDelete = (dto: TDto) => setDeleting(dto);
  const closeDelete = () => setDeleting(null);

  const runDelete = async () => {
    if (!deleting) return;
    try {
      await deleteMutation.mutateAsync(deleting.id);
      onDeleted?.(deleting);
      closeDelete();
    } catch {
      // toast already shown
    }
  };

  return {
    createMutation,
    updateMutation,
    deleteMutation,
    busy,
    open,
    edit,
    deleting,
    form,
    setForm,
    openCreate,
    openEdit,
    closeDialog,
    handleSave,
    confirmDelete,
    closeDelete,
    runDelete,
  };
}
