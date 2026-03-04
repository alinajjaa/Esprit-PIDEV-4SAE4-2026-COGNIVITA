import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UserService, User } from '../../services/user.service';
import { UsersListComponent } from '../../components/users-list/users-list';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, UsersListComponent],
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.css']
})
export class AdminDashboardComponent implements OnInit {

  users: User[] = [];
  loading = false;
  error: string | null = null;
  today = new Date();

  constructor(
    private userService: UserService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadDashboard();
  }

  loadDashboard(): void {
    this.loading = true;
    this.error = null;
    this.cdr.detectChanges();

    this.userService.getAll().subscribe({
      next: (data: User[]) => {
        this.users = [...data]; // ✅ nouveau tableau pour forcer ngOnChanges
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err: any) => {
        console.error('Error loading users', err);
        this.error = 'Failed to load users';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }
}