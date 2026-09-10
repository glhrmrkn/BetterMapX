package dev.bettermapx;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

final class ConfigSlider extends SliderWidget {
    private final DoubleConsumer change; private final DoubleFunction<String> label; private final double min,max;
    ConfigSlider(int x,int y,int width,String name,double value,double min,double max,DoubleConsumer change,DoubleFunction<String> label){
        super(x,y,width,20,Text.empty(),(value-min)/(max-min)); this.change=change;this.label=label;this.min=min;this.max=max;updateMessage();
    }
    private double actual(){return min+value*(max-min);}
    @Override protected void updateMessage(){setMessage(Text.literal(label.apply(actual())));}
    @Override protected void applyValue(){change.accept(actual());}
}
