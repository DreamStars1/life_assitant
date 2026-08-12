// commentImageGallery.ts
export type CommentImageSource = {
  id?: string
  imageUrls?: string[] | null
}

export function buildCommentImageGallery(
  comments: CommentImageSource[],
  activeCommentId: string | undefined,
  localIndex: number,
): { images: string[]; startPosition: number } {
  const images: string[] = []
  let startPosition = 0
  let anchored = false

  for (const c of comments) {
    const urls = (c.imageUrls ?? []).filter((u): u is string => !!u)
    if (
      !anchored
      && activeCommentId
      && c.id === activeCommentId
      && localIndex >= 0
      && localIndex < urls.length
    ) {
      startPosition = images.length + localIndex
      anchored = true
    }
    images.push(...urls)
  }

  if (!anchored)
    startPosition = 0

  return { images, startPosition }
}
