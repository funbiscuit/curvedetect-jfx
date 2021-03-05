clear, clc, close all

f=@(x) 10+x.^2+5*x.*sin(x);

figure(1);
fplot(f, [0 10])
grid on
legend('y=10+x^2+5*x*sin(x)','location','northwest')
