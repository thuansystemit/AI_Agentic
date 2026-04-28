import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { VideoService } from '../../shared/services/video.service';
import { VideoResponse } from '../../shared/models/video.model';
import { VideoCardComponent } from '../../shared/components/video-card/video-card.component';

@Component({
  selector: 'app-my-videos',
  standalone: true,
  imports: [RouterLink, VideoCardComponent],
  templateUrl: './my-videos.component.html',
  styleUrl: './my-videos.component.scss',
})
export class MyVideosComponent implements OnInit {
  videos = signal<VideoResponse[]>([]);
  loading = signal(true);
  currentPage = signal(0);
  totalPages = signal(0);
  totalElements = signal(0);

  constructor(private videoService: VideoService) {}

  ngOnInit(): void {
    this.loadMyVideos();
  }

  loadMyVideos(): void {
    this.loading.set(true);
    this.videoService.getMyVideos(this.currentPage(), 12).subscribe({
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

  nextPage(): void {
    if (this.currentPage() < this.totalPages() - 1) {
      this.currentPage.update((p) => p + 1);
      this.loadMyVideos();
    }
  }

  prevPage(): void {
    if (this.currentPage() > 0) {
      this.currentPage.update((p) => p - 1);
      this.loadMyVideos();
    }
  }
}
