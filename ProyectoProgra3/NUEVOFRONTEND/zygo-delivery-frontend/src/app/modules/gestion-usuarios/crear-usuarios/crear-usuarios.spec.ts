import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CrearUsuariosComponent } from './crear-usuarios';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';

describe('CrearUsuariosComponent', () => {
  let component: CrearUsuariosComponent;
  let fixture: ComponentFixture<CrearUsuariosComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CrearUsuariosComponent],
      providers: [
        provideHttpClient(),
        provideRouter([])
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CrearUsuariosComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have empty usuario initially', () => {
    const usuario = component.usuario();
    expect(usuario.nombre).toBe('');
    expect(usuario.email).toBe('');
  });
});