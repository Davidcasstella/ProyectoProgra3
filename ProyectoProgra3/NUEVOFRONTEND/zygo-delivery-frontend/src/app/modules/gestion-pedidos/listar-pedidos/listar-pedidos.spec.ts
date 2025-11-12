import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ListarPedidos } from './listar-pedidos';

describe('ListarPedidos', () => {
  let component: ListarPedidos;
  let fixture: ComponentFixture<ListarPedidos>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ListarPedidos]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ListarPedidos);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
