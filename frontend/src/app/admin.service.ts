import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UserAccount, UserRole } from './model/user.model';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly apiUrl = '/api/admin/users';

  constructor(private readonly http: HttpClient) {}

  getUsers(): Observable<UserAccount[]> {
    return this.http.get<UserAccount[]>(this.apiUrl);
  }

  updateUser(id: string, update: { role?: UserRole; enabled?: boolean }): Observable<UserAccount> {
    return this.http.patch<UserAccount>(`${this.apiUrl}/${id}`, update);
  }
}
