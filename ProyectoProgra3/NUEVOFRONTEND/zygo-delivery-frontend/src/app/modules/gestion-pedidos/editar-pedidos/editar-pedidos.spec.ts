import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EditarPedidos } from './editar-pedidos';

describe('EditarPedidos', () => {
  let component: EditarPedidos;
  let fixture: ComponentFixture<EditarPedidos>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EditarPedidos]
    })
    .compileComponents();

    fixture = TestBed.createComponent(EditarPedidos);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
