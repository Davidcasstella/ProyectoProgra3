import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CrearPedidos } from './crear-pedidos';

describe('CrearPedidos', () => {
  let component: CrearPedidos;
  let fixture: ComponentFixture<CrearPedidos>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CrearPedidos]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CrearPedidos);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
