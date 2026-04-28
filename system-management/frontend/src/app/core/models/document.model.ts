export interface DocumentResponse {
  id: string;
  title: string;
  content: string;
  categoryId: string;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
}

export interface UpdateDocumentRequest {
  title?: string;
  content?: string;
}
