// src/app/cognitive-activities/journal/journal.component.ts
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-journal',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './journal.component.html',
  styleUrls: ['./journal.component.css']
})
export class JournalComponent { }
