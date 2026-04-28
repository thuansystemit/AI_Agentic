import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { VideoService } from '../../shared/services/video.service';
import { VideoResponse } from '../../shared/models/video.model';
import { VideoCardComponent } from '../../shared/components/video-card/video-card.component';

@Component({
  selector: 'app-video-list',
  standalone: true,
  imports: [FormsModule, VideoCardComponent],
  templateUrl: './video-list.component.html',
  styleUrl: './video-list.component.scss',
})
export class VideoListComponent implements OnInit {
  videos = signal<VideoResponse[]>([]);
  loading = signal(true);
  searchQuery = '';
  currentPage = signal(0);
  totalPages = signal(0);
  totalElements = signal(0);

  constructor(private videoService: VideoService) {}

  ngOnInit(): void {
    this.loadVideos();
  }

  loadVideos(): void {
    this.loading.set(true);
    this.videoService.listVideos(this.currentPage(), 12, this.searchQuery).subscribe({
      next: (response) => {
        this.videos.set(response.content);
        this.totalPages.set(response.totalPages);
        this.totalElements.set(response.totalElements);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      },
    });
  }

  onSearch(): void {
    this.currentPage.set(0);
    this.loadVideos();
  }

  onClearSearch(): void {
    this.searchQuery = '';
    this.currentPage.set(0);
    this.loadVideos();
  }

  nextPage(): void {
    if (this.currentPage() < this.totalPages() - 1) {
      this.currentPage.update((p) => p + 1);
      this.loadVideos();
    }
  }

  prevPage(): void {
    if (this.currentPage() > 0) {
      this.currentPage.update((p) => p - 1);
      this.loadVideos();
    }
  }
}
