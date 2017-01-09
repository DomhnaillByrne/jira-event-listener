package ut.com.domhnaill.plugins;

import org.junit.Test;

import com.domhnaill.plugins.MyPluginComponent;
import com.domhnaill.plugins.MyPluginComponentImpl;

import static org.junit.Assert.assertEquals;

public class MyComponentUnitTest
{
    @Test
    public void testMyName()
    {
        MyPluginComponent component = new MyPluginComponentImpl(null);
        assertEquals("names do not match!", "myComponent",component.getName());
    }
}