/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 PaleoCrafter, Ordinastie
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.malisis.ego.gui.component;

import static com.google.common.base.Preconditions.*;

import net.malisis.ego.gui.EGOGui;
import net.malisis.ego.gui.component.container.UIContainer;
import net.malisis.ego.gui.component.content.IContent;
import net.malisis.ego.gui.component.control.IControlComponent;
import net.malisis.ego.gui.component.decoration.UITooltip;
import net.malisis.ego.gui.element.IClipable;
import net.malisis.ego.gui.element.IClipable.ClipArea;
import net.malisis.ego.gui.element.IKeyListener;
import net.malisis.ego.gui.element.IOffset;
import net.malisis.ego.gui.element.ISpace;
import net.malisis.ego.gui.element.Margin;
import net.malisis.ego.gui.element.Padding;
import net.malisis.ego.gui.element.Padding.IPadded;
import net.malisis.ego.gui.element.position.Position;
import net.malisis.ego.gui.element.position.Position.IPosition;
import net.malisis.ego.gui.element.position.Position.ScreenPosition;
import net.malisis.ego.gui.element.size.Size;
import net.malisis.ego.gui.element.size.Size.ISize;
import net.malisis.ego.gui.element.size.Sizes;
import net.malisis.ego.gui.event.EventHandler;
import net.malisis.ego.gui.event.GuiEvent;
import net.malisis.ego.gui.event.KeyTypedEvent;
import net.malisis.ego.gui.event.StateChangeEvent.FocusEvent;
import net.malisis.ego.gui.event.StateChangeEvent.UnfocusEvent;
import net.malisis.ego.gui.event.mouse.IMouseEventRegister;
import net.malisis.ego.gui.event.mouse.IMouseListener;
import net.malisis.ego.gui.event.mouse.MouseEvent.MouseDoubleClick;
import net.malisis.ego.gui.event.mouse.MouseEvent.MouseDown;
import net.malisis.ego.gui.event.mouse.MouseEvent.MouseDrag;
import net.malisis.ego.gui.event.mouse.MouseEvent.MouseLeftClick;
import net.malisis.ego.gui.event.mouse.MouseEvent.MouseMove;
import net.malisis.ego.gui.event.mouse.MouseEvent.MouseOut;
import net.malisis.ego.gui.event.mouse.MouseEvent.MouseOver;
import net.malisis.ego.gui.event.mouse.MouseEvent.MouseRightClick;
import net.malisis.ego.gui.event.mouse.MouseEvent.MouseUp;
import net.malisis.ego.gui.event.mouse.MouseEvent.ScrollWheel;
import net.malisis.ego.gui.render.GuiRenderer;
import net.malisis.ego.gui.render.IGuiRenderer;
import net.minecraft.util.text.TextFormatting;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

import javax.annotation.Nonnull;

/**
 * {@link UIComponent} is the base of everything drawn onto a GUI.<br>
 * The drawing is separated between background and foreground.<br>
 * Most of the events are launched from UIComponent.
 *
 * @author Ordinastie
 */
public class UIComponent implements IContent, IMouseListener, IKeyListener, IMouseEventRegister<UIComponent>, IPadded
{
	/** Reference to the {@link EGOGui} this {@link UIComponent} was added to. Set when the component is added to screen. */
	protected EGOGui gui;
	/** Event handler. */
	protected final EventHandler eventHandler = new EventHandler();
	/** List of {@link UIComponent components} controlling this {@link UIContainer}. */
	protected final Set<IControlComponent> controlComponents;
	/** Position of this {@link UIComponent}. */
	protected IPosition position = Position.topLeft(this);
	/** Position of this {@link UIComponent} on screen. */
	private final IPosition screenPosition = new ScreenPosition(this);
	/** Position of the mouse inside this {@link UIComponent}. */
	private final IPosition mousePosition = Position.of(this::mouseX, this::mouseY);
	/** Size of this {@link UIComponent}. */
	protected ISize size = Size.inherited(this);
	/** Size available for content. */
	protected ISize innerSize = Size.of(Sizes.innerWidth(this), Sizes.innerHeight(this));
	/** Padding of this component. */
	protected Padding padding = Padding.NO_PADDING;
	/** Margin of this component. */
	protected Margin margin = EGOGui.defaultMargin();
	/** Z index of the component. */
	protected int zIndex = 0;
	/** Color of the component. Effect dependent on renderers. */
	protected int color = 0xFFFFFF;
	/** Alpha transparency of this {@link UIComponent}. */
	protected int alpha = 255;
	/** The parent {@link UIComponent} of this <code>UIComponent</code>. */
	protected UIComponent parent;
	/** The name of this {@link UIComponent}. Can be used to retrieve this back from a parent. */
	protected String name;
	/** The tooltip for this {@link UIComponent} Automatically displayed when the {@link UIComponent} is hovered. */
	protected UITooltip tooltip;
	/** Determines whether this {@link UIComponent} is visible. */
	protected BooleanSupplier visible = () -> true;
	/** Determines whether this {@link UIComponent} is enabled. If set to false, will cancel any {@link GuiEvent events} received. */
	protected BooleanSupplier enabled = () -> true;
	/** Hover state of this {@link UIComponent}. */
	protected boolean hovered = false;
	/** Focus state of this {@link UIComponent}. */
	protected boolean focused = false;

	/** Rendering for the background of this {@link UIComponent}. */
	protected IGuiRenderer backgroundRenderer = null;
	/** Rendering for the foreground of this {@link UIComponent}. */
	protected IGuiRenderer foregroundRenderer = null;

	private Object data;

	/**
	 * Instantiates a new {@link UIComponent}.
	 */
	public UIComponent()
	{
		controlComponents = new LinkedHashSet<>();
	}

	// #region getters/setters

	/**
	 * Gets the {@link EGOGui} this {@link UIComponent} was added to.
	 *
	 * @return the gui
	 */
	public EGOGui getGui()
	{
		return gui;
	}

	/**
	 * Sets the position of this {@link UIComponent}.
	 *
	 * @param position the new position
	 */
	public void setPosition(@Nonnull IPosition position)
	{
		//if(fireEvent(this, this.position, position);
		this.position = checkNotNull(position);
	}

	/**
	 * Gets the position of this {@link UIComponent}.
	 *
	 * @return the position
	 */
	@Override
	@Nonnull
	public IPosition position()
	{
		return position;
	}

	@Override
	public boolean fixed()
	{
		return this instanceof IControlComponent;
	}

	/**
	 * Gets the position of this {@link UIComponent} on the screen
	 *
	 * @return the component position
	 */
	public IPosition screenPosition()
	{
		return screenPosition;
	}

	/**
	 * Gets the position of the mouse inside this {@link UIComponent}
	 *
	 * @return mouse position
	 */
	public IPosition mousePosition()
	{
		return mousePosition;
	}

	private int mouseX()
	{
		return EGOGui.MOUSE_POSITION.x() - screenPosition.x() - IOffset.x(this);
	}

	private int mouseY()
	{
		return EGOGui.MOUSE_POSITION.y() - screenPosition.y() - IOffset.x(this);
	}

	/**
	 * Sets the size of this {@link UIComponent}.
	 *
	 * @param size the new size
	 */
	public void setSize(@Nonnull ISize size)
	{
		//if(fireEvent(this, this.size, size)
		this.size = checkNotNull(size);
	}

	/**
	 * Gets the size of this {@link UIComponent}.
	 *
	 * @return the size
	 */
	@Override
	@Nonnull
	public ISize size()
	{
		return size;
	}

	@Override
	@Nonnull
	public ISize innerSize()
	{
		return innerSize;
	}

	public void setPadding(Padding padding)
	{
		if (padding == null)
			padding = Padding.NO_PADDING;
		this.padding = padding;
	}

	@Nonnull
	@Override
	public Padding padding()
	{
		return padding;
	}

	public void setMargin(Margin margin)
	{
		if (margin == null)
			margin = Margin.NO_MARGIN;
		this.margin = margin;
	}

	@Nonnull
	public Margin margin()
	{
		return margin;
	}

	public int controlSpace(ToIntFunction<ISpace> func)
	{
		if (controlComponents.size() == 0)
			return 0;
		return controlComponents.stream()
								.mapToInt(func)
								.max()
								.orElse(0);
	}

	/**
	 * Sets the zIndex for this {@link UIComponent}.
	 *
	 * @param zIndex the z index
	 */
	public void setZIndex(int zIndex)
	{
		this.zIndex = zIndex;
	}

	/**
	 * Gets the zIndex of this {@link UIComponent}.
	 *
	 * @return the zIndex
	 */
	public int zIndex()
	{
		return zIndex == 0 ? (parent != null ? parent.zIndex() : 0) : zIndex;
	}

	/**
	 * Hovers this {@link UIComponent}.
	 */
	public void hover()
	{
		fireEvent(new MouseOver<>(this));
	}

	/**
	 * Unhovers this {@link UIComponent}.
	 */
	public void unhover()
	{
		fireEvent(new MouseOut<>(this));
	}

	/**
	 * Gets the <code>hovered</code> state of this {@link UIComponent}.
	 *
	 * @return true, this component is hovered
	 */
	public boolean isHovered()
	{
		return isVisible() && EGOGui.getHoveredComponent() == this;
	}

	/**
	 * Set focus on this {@link UIComponent}.
	 */
	public void focus()
	{
		fireEvent(new FocusEvent<>(this));
	}

	/**
	 * Unset the focus on this {@link UIComponent}
	 */
	public void unfocus()
	{
		fireEvent(new UnfocusEvent<>(this));
	}

	/**
	 * Gets the <code>focused</code> state of this {@link UIComponent}.
	 *
	 * @return true, if this component if focused
	 */
	public boolean isFocused()
	{
		return isVisible() && EGOGui.getFocusedComponent() == this;
	}

	/**
	 * Gets the parent of this {@link UIComponent}.
	 *
	 * @return the parent
	 */
	@Override
	public UIComponent getParent()
	{
		return parent;
	}

	/**
	 * Sets the parent of this {@link UIComponent}.
	 *
	 * @param parent the parent
	 */
	public void setParent(UIComponent parent)
	{
		this.parent = parent;
		//fireEvent(new ContentUpdate<>(this));
	}

	/**
	 * Checks if this {@link UIComponent} is visible.
	 *
	 * @return true, if visible
	 */
	public boolean isVisible()
	{
		return visible.getAsBoolean();
	}

	/**
	 * Sets the visibility rule of this {@link UIComponent}.
	 *
	 * @param supplier the visibility rule for this component
	 */
	public void setVisible(BooleanSupplier supplier)
	{
		visible = checkNotNull(supplier);
	}

	/**
	 * Sets the visibility of this {@link UIComponent}.
	 *
	 * @param visible the visibility for this component
	 */
	public void setVisible(boolean visible)
	{
		setVisible(() -> visible);
	}

	/**
	 * Checks if this {@link UIComponent} is enabled.
	 *
	 * @return true if enabled
	 */
	public boolean isEnabled()
	{
		return enabled.getAsBoolean() && (parent == null || parent.isEnabled());
	}

	/**
	 * Checks if this {@link UIComponent} is disabled.
	 *
	 * @return true, if is disabled
	 */
	public boolean isDisabled()
	{
		return !isEnabled();
	}

	/**
	 * Set the state of this {@link UIComponent}.
	 *
	 * @param supplier the new state
	 */
	public void setEnabled(BooleanSupplier supplier)
	{
		enabled = checkNotNull(supplier);
	}

	/**
	 * Set the state of this {@link UIComponent}.
	 *
	 * @param enabled the new state
	 */
	public void setEnabled(boolean enabled)
	{
		setEnabled(() -> enabled);
	}

	/**
	 * Gets the name of this {@link UIComponent}.
	 *
	 * @return the name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Sets the name of this {@link UIComponent}.
	 *
	 * @param name the name to be used
	 */
	public void setName(String name)
	{
		this.name = name;
	}

	/**
	 * Gets the {@link UITooltip} for this {@link UIComponent}.
	 *
	 * @return the tooltip
	 */
	public UITooltip getTooltip()
	{
		return tooltip;
	}

	/**
	 * Sets the {@link UITooltip} of this {@link UIComponent}.
	 *
	 * @param tooltip the tooltip
	 */
	public void setTooltip(UITooltip tooltip)
	{
		this.tooltip = tooltip;
	}

	/**
	 * Sets the {@link UITooltip} of this {@link UIComponent}.
	 *
	 * @param text the text of the tooltip
	 */
	public void setTooltip(String text)
	{
		if (Strings.isEmpty(text))
			setTooltip((UITooltip) null);
		else
			setTooltip(new UITooltip(text));
	}

	//@Override
	public void setColor(int color)
	{
		this.color = color;
	}

	public int getColor()
	{
		return color;
	}

	/**
	 * Sets the alpha transparency for this {@link UIComponent}.
	 *
	 * @param alpha the new alpha
	 */
	//@Override
	public void setAlpha(int alpha)
	{
		this.alpha = alpha;
	}

	/**
	 * Gets the alpha transparency for this {@link UIComponent}.
	 *
	 * @return the alpha
	 */
	public int getAlpha()
	{
		if (getParent() == null)
			return alpha;

		return Math.min(alpha, parent.getAlpha());
	}

	/**
	 * Sets the background for this {@link UIComponent}.
	 *
	 * @param render the new background
	 */
	public void setBackground(IGuiRenderer render)
	{
		backgroundRenderer = render;
	}

	/**
	 * Sets the foreground for this {@link UIComponent}.
	 *
	 * @param render the new foreground
	 */
	public void setForeground(IGuiRenderer render)
	{
		foregroundRenderer = render;
	}

	public void attachData(Object data)
	{
		this.data = data;
	}

	public Object getData()
	{
		return data;
	}

	// #end getters/setters

	/**
	 * Fires a {@link GuiEvent}.<br>
	 *
	 * @param event event to fire
	 * @return if true, stop the propagation of the event to the source's parent
	 */
	public boolean fireEvent(GuiEvent<?> event)
	{
		return eventHandler.fireEvent(event);
	}

	@Override
	public <T extends GuiEvent<? extends UIComponent>> void register(Class<T> clazz, Predicate<T> handler)
	{
		eventHandler.register(clazz, handler);
	}

	public void onKeyTyped(Predicate<KeyTypedEvent<UIComponent>> handler)
	{
		register(KeyTypedEvent.class, (Predicate) handler);
	}

	//#region Inputs

	/**
	 * Called from the GUI when mouse is moved and over this component.
	 */
	@Override
	public void mouseMove()
	{
		if (!isEnabled())
			return;
		if (fireEvent(new MouseMove<>(this)))
			return;
		if (parent != null)
			parent.mouseMove();
	}

	/**
	 * Called from the GUI when a mouse button is pressed
	 *
	 * @param button the button
	 */
	@Override
	public void mouseDown(MouseButton button)
	{
		if (!isEnabled())
			return;
		if (fireEvent(new MouseDown<>(this, button)))
			return;
		if (parent != null)
			parent.mouseDown(button);
	}

	/**
	 * Called from teh GUI when a mouse button is released
	 *
	 * @param button the button
	 */
	@Override
	public void mouseUp(MouseButton button)
	{
		if (!isEnabled())
			return;
		if (fireEvent(new MouseUp<>(this, button)))
			return;
		if (parent != null)
			parent.mouseUp(button);
	}

	/**
	 * Called from the GUI when the left mouse button is clicked
	 *
	 * @param button button being clicked
	 */
	@Override
	public void click(MouseButton button)
	{
		if (!isEnabled())
			return;
		if (fireEvent(button == MouseButton.LEFT ? new MouseLeftClick<>(this) : new MouseRightClick<>(this)))
			return;
		if (parent != null)
			parent.click(button);
	}

	/**
	 * Called from the GUI when a mouse button is double clicked.<br>
	 * Only fires the event if the button is {@link MouseButton#LEFT}.
	 *
	 * @param button the button
	 */
	@Override
	public void doubleClick(MouseButton button)
	{
		if (!isEnabled())
			return;
		if (button != MouseButton.LEFT)
			return;
		if (fireEvent(new MouseDoubleClick<>(this)))
			return;
		if (parent != null)
			parent.doubleClick(button);
	}

	/**
	 * Called from the GUI when the mouse is dragged with a button pressed.<br>
	 * To get the distance dragged, use {@code EGOGui.MOUSE_POSITION.moved()}
	 *
	 * @param button the button
	 */
	@Override
	public void mouseDrag(MouseButton button)
	{
		if (!isEnabled())
			return;
		if (fireEvent(new MouseDrag<>(this, button)))
			return;
		if (parent != null)
			parent.mouseDrag(button);
	}

	@Override
	public boolean dragPreventsClick()
	{
		return false;
	}

	/**
	 * Called from the GUI when the mouse scroll wheel is used.
	 *
	 * @param delta the delta
	 */
	@Override
	public void scrollWheel(int delta)
	{
		if (!isEnabled())
			return;

		if (fireEvent(new ScrollWheel<>(this, delta)))
			return;

		if (parent != null)
			parent.scrollWheel(delta);
	}

	@Override
	public boolean keyTyped(char keyChar, int keyCode)
	{
		if (!isEnabled())
			return false;

		if (fireEvent(new KeyTypedEvent<>(this, keyChar, keyCode)))
			return true;

		return parent != null && parent.keyTyped(keyChar, keyCode);
	}

	//#end Inputs

	/**
	 * Checks if supplied coordinates are inside this {@link UIComponent} bounds.
	 *
	 * @param x the x
	 * @param y the y
	 * @return true, if coordinates are inside bounds
	 */
	public boolean isInsideBounds(int x, int y)
	{
		if (!isVisible())
			return false;
		int sx = screenPosition().x();
		int sy = screenPosition().y();
		return x >= sx && x < sx + size().width() && y >= sy && y < sy + size().height();
	}

	/**
	 * Gets the {@link UIComponent} at the specified coordinates.<br>
	 * Will return a {@link IControlComponent} if any. Checks if inside bounds, visible and not disabled.
	 *
	 * @param x the x
	 * @param y the y
	 * @return this {@link UIComponent} or null if outside its bounds.
	 */
	public UIComponent getComponentAt(int x, int y)
	{
		//control components take precedence over regular components
		for (IControlComponent c : controlComponents)
		{
			if (c instanceof UIComponent && c.isInsideBounds(x, y))
				return (UIComponent) c;
		}

		return isInsideBounds(x, y) ? this : null;
	}

	public UIComponent getComponent(String name)
	{
		if (StringUtils.isEmpty(name))
			return null;

		if (name.equals(getName()))
			return this;

		UIComponent found;
		for (IControlComponent c : controlComponents)
			if (c instanceof UIComponent && (found = ((UIComponent) c).getComponent(name)) != null)
				return found;

		return null;
	}

	/**
	 * Adds a {@link IControlComponent} component to this {@link UIComponent}.
	 *
	 * @param component the component
	 */
	public void addControlComponent(IControlComponent component)
	{
		controlComponents.add(component);
		component.setParent(this);
	}

	/**
	 * Removes the {@link IControlComponent} from this {@link UIComponent}.
	 *
	 * @param component the component
	 */
	public void removeControlComponent(IControlComponent component)
	{
		if (component.getParent() != this)
			return;

		controlComponents.remove(component);
		component.setParent(null);
	}

	/**
	 * Removes all the {@link IControlComponent} from this {@link UIContainer}.
	 */
	public void removeAllControlComponents()
	{
		for (IControlComponent component : controlComponents)
		{
			component.setParent(null);
		}
		controlComponents.clear();
	}

	/**
	 * Called when this {@link UIComponent} is added to screen.
	 */
	public void onAddedToScreen(EGOGui gui)
	{
		this.gui = gui;
	}

	/**
	 * Draws this {@link UIComponent}. Rendering is surrounded by glPushAttrib(GL_ALL_ATTRIB_BITS) so no state should bleed between
	 * components. Also, a draw() is triggered between background and foreground.
	 *
	 * @param renderer the renderer
	 */
	@Override
	public void render(GuiRenderer renderer)
	{
		if (!isVisible())
			return;

		//		if (getGui().isOverlay())
		//		{
		//			GlStateManager.blendFunc(GL11.GL_CONSTANT_ALPHA, GL11.GL_ONE_MINUS_CONSTANT_ALPHA);
		//			GL14.glBlendColor(1, 1, 1, (float) getAlpha() / 255);
		//		}

		//store last drawn component so that it can be set back after drawing.
		//makes sure components overriding rendering and calling super still have correct
		//relative position in case super renders other components.
		UIComponent oldComponent = renderer.currentComponent;

		//draw background
		if (backgroundRenderer != null)
		{
			renderer.currentComponent = this;
			backgroundRenderer.render(renderer);
		}

		//draw foreground
		if (foregroundRenderer != null)
		{
			ClipArea area = IClipable.intersected(this);
			if (!area.fullClip())
			{
				if (!(this instanceof IControlComponent))
					renderer.startClipping(area);
				renderer.currentComponent = this;
				foregroundRenderer.render(renderer);
				if (!(this instanceof IControlComponent))
					renderer.endClipping(area);

			}
		}

		if (controlComponents.size() != 0)
		{
			controlComponents.forEach(c -> c.render(renderer));
		}

		renderer.currentComponent = oldComponent;
	}

	/**
	 * Gets the property string.
	 *
	 * @return the property string
	 */
	public String getPropertyString()
	{
		String str = position() + "@" + size() + " | Screen=" + screenPosition() + " | ";
		str += Padding.of(this) + ", ";
		str += Margin.of(this);
		return str;
	}

	/**
	 * To string.
	 *
	 * @return the string
	 */
	@Override
	public String toString()
	{
		return (name == null ? getClass().getSimpleName() : TextFormatting.ITALIC + name + TextFormatting.RESET) + " "
				+ getPropertyString();
	}

	public static BaseComponentBuilder base()
	{
		return new BaseComponentBuilder();
	}

	public static class BaseComponentBuilder extends UIComponentBuilder<BaseComponentBuilder, UIComponent>
	{
		@Override
		public UIComponent build()
		{
			return build(new UIComponent());
		}
	}
}
