import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { VideoService } from '../../shared/services/video.service';

@Component({
  selector: 'app-video-upload',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './video-upload.component.html',
  styleUrl: './video-upload.component.scss',
})
export class VideoUploadComponent {
  title = '';
  description = '';
  selectedFile: File | null = null;
  error = signal<string | null>(null);
  loading = signal(false);
  dragOver = signal(false);

  private readonly allowedTypes = [
    'video/mp4',
    'video/webm',
    'video/x-msvideo',
    'video/quicktime',
    'video/x-matroska',
  ];
  private readonly maxSize = 500 * 1024 * 1024; // 500MB

  constructor(
    private videoService: VideoService,
    private router: Router,
  ) {}

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.validateAndSetFile(input.files[0]);
    }
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragOver.set(true);
  }

  onDragLeave(): void {
    this.dragOver.set(false);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragOver.set(false);
    if (event.dataTransfer?.files && event.dataTransfer.files.length > 0) {
      this.validateAndSetFile(event.dataTransfer.files[0]);
    }
  }

  private validateAndSetFile(file: File): void {
    this.error.set(null);

    if (!this.allowedTypes.includes(file.type)) {
      this.error.set('Invalid file type. Supported formats: MP4, WebM, AVI, MOV, MKV');
      return;
    }

    if (file.size > this.maxSize) {
      this.error.set('File too large. Maximum size is 500MB.');
      return;
    }

    this.selectedFile = file;
    if (!this.title) {
      // Auto-fill title from filename
      this.title = file.name.replace(/\.[^/.]+$/, '').replace(/[-_]/g, ' ');
    }
  }

  removeFile(): void {
    this.selectedFile = null;
  }

  formatFileSize(bytes: number): string {
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  }

  onSubmit(): void {
    if (!this.selectedFile) {
      this.error.set('Please select a video file');
      return;
    }

    if (!this.title.trim()) {
      this.error.set('Please enter a title');
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    this.videoService.uploadVideo(this.selectedFile, this.title, this.description).subscribe({
      next: (video) => {
        this.router.navigate(['/videos', video.id]);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.message || 'Upload failed. Please try again.');
      },
    });
  }
}
