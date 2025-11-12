import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ListarUsuariosComponent } from './listar-usuarios';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';

describe('ListarUsuariosComponent', () => {
  let component: ListarUsuariosComponent;
  let fixture: ComponentFixture<ListarUsuariosComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ListarUsuariosComponent],
      providers: [
        provideHttpClient(),
        provideRouter([])
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ListarUsuariosComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with empty arrays', () => {
    expect(component.usuarios()).toEqual([]);
    expect(component.usuariosFiltrados()).toEqual([]);
  });

  it('should set loading to true initially', () => {
    expect(component.loading()).toBe(true);
  });
});