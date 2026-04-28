import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { VideoService } from '../../shared/services/video.service';
import { AuthService } from '../../shared/services/auth.service';
import { VideoResponse } from '../../shared/models/video.model';

@Component({
  selector: 'app-video-detail',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './video-detail.component.html',
  styleUrl: './video-detail.component.scss',
})
export class VideoDetailComponent implements OnInit {
  video = signal<VideoResponse | null>(null);
  loading = signal(true);
  error = signal<string | null>(null);
  editing = signal(false);
  editTitle = '';
  editDescription = '';
  confirmDelete = signal(false);

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private videoService: VideoService,
    public authService: AuthService,
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) {
      this.loadVideo(id);
    }
  }

  loadVideo(id: number): void {
    this.loading.set(true);
    this.videoService.getVideo(id).subscribe({
      next: (video) => {
        this.video.set(video);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Video not found');
        this.loading.set(false);
      },
    });
  }

  getStreamUrl(): string {
    const v = this.video();
    return v ? this.videoService.getStreamUrl(v.id) : '';
  }

  isOwner(): boolean {
    const v = this.video();
    const user = this.authService.currentUser();
    return v !== null && user !== null && v.owner.id === user.id;
  }

  startEdit(): void {
    const v = this.video();
    if (v) {
      this.editTitle = v.title;
      this.editDescription = v.description || '';
      this.editing.set(true);
    }
  }

  cancelEdit(): void {
    this.editing.set(false);
  }

  saveEdit(): void {
    const v = this.video();
    if (!v) return;

    this.videoService
      .updateVideo(v.id, {
        title: this.editTitle,
        description: this.editDescription,
      })
      .subscribe({
        next: (updated) => {
          this.video.set(updated);
          this.editing.set(false);
        },
        error: (err) => {
          this.error.set(err.error?.message || 'Failed to update video');
        },
      });
  }

  deleteVideo(): void {
    const v = this.video();
    if (!v) return;

    this.videoService.deleteVideo(v.id).subscribe({
      next: () => {
        this.router.navigate(['/my-videos']);
      },
      error: (err) => {
        this.error.set(err.error?.message || 'Failed to delete video');
        this.confirmDelete.set(false);
      },
    });
  }

  formatFileSize(bytes: number): string {
    if (!bytes) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }
}
