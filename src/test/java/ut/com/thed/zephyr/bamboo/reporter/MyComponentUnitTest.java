package ut.com.thed.zephyr.bamboo.reporter;

import org.junit.Test;
import com.thed.zephyr.bamboo.reporter.MyPluginComponent;
import com.thed.zephyr.bamboo.reporter.MyPluginComponentImpl;

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