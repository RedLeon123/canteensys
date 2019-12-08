package com.lckgroup.canteensys.controller;

import com.lckgroup.canteensys.entity.Customer;
import com.lckgroup.canteensys.entity.Dish;
import com.lckgroup.canteensys.entity.OrderItem;
import com.lckgroup.canteensys.entity.Orders;
import com.lckgroup.canteensys.service.CustomerService;
import com.lckgroup.canteensys.service.DishService;
import com.lckgroup.canteensys.service.OrdersService;
import com.lckgroup.canteensys.service.OrderItemService;
import com.lckgroup.canteensys.util.ControllerUtil;
import com.lckgroup.canteensys.util.constant.RespCode;
import com.lckgroup.canteensys.vo.ResponseVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
//解决ajax跨域问题
@CrossOrigin
@RequestMapping("/customer")
@Api(tags ={"顾客操作"})
public class CustomerController {

    public static final Logger logger = LoggerFactory.getLogger(CustomerController.class);

    @Autowired
    private CustomerService customerService;

    @Autowired
    private OrdersService ordersService;

    @Autowired
    private DishService dishService;


    @Autowired
    private OrderItemService orderItemService;


    @ApiOperation(value = "通过卡号获得顾客", httpMethod = "GET")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "cusId", value = "卡号", required = true)
    })
    @GetMapping("/{cusId}")
    public ResponseVO findByCusId(@PathVariable String cusId){
        Customer customer = customerService.findByCusId(cusId);
        if(customer==null){
            return ControllerUtil.getFalseResultMsgBySelf(RespCode.MSG_NOT_FOUND_DATA);
        }
        customer.setCusPwd("");
        return ControllerUtil.getDataResult(customer);
    }

    @ApiOperation(value = "创建新顾客",httpMethod = "POST" )
    @PostMapping("/create")
    public ResponseVO createCustomer(Customer customer){
        return ControllerUtil.getSuccessResultBySelf(customerService.createCustomer(customer));
    }

    @ApiOperation(value = "查看所有顾客信息",httpMethod = "GET")
    @GetMapping("/allCustomer")
    public ResponseVO findAllCustomer(){
        List<Customer> cusList = customerService.findAllCustomer();
        if(cusList.isEmpty()){
            return ControllerUtil.getFalseResultMsgBySelf(RespCode.MSG_NOT_FOUND_DATA);
        }
        for(Customer customer:cusList){
            customer.setCusPwd("");
        }
        return ControllerUtil.getDataResult(cusList);
    }

    @ApiOperation(value = "顾客查看当日所有在售菜品",httpMethod = "GET")
    @GetMapping("/allDish")
    public ResponseVO findAllDish(){
        List<Dish> dishList = dishService.findAllDish();
        if(dishList.isEmpty()){
            return ControllerUtil.getFalseResultMsgBySelf(RespCode.MSG_NOT_FOUND_DATA);
        }
        return ControllerUtil.getDataResult(dishList);
    }

    @ApiOperation(value = "通过卡号查看订单",httpMethod = "GET")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "cusId", value = "卡号", required = true)
    })
    @GetMapping("/customerOrder/{cusId}")
    public ResponseVO findAll(@PathVariable String cusId){
        List<Orders> ordersList = ordersService.findOrdersByCusId(cusId);
        if(ordersList.isEmpty()){
            return ControllerUtil.getFalseResultMsgBySelf(RespCode.MSG_NOT_FOUND_DATA);
        }
        return ControllerUtil.getDataResult(ordersList);
    }

    @ApiOperation(value = "顾客点餐提交订单",httpMethod = "POST" )
    @PostMapping("/customerOrder/creatOrder")
    public ResponseVO creatOrder(@RequestBody List<OrderItem> orderItems){
        if(orderItems.isEmpty()||orderItems==null){
            return ControllerUtil.getFalseResultMsgBySelf(RespCode.MSG_NOT_FOUND_DATA);
        }
        //模拟已登录顾客
        Customer customer = new Customer();
        String cusId = customer.getCusId();
        //新建订单类
        Orders orders = new Orders(new Long(0),cusId, new Float(0),new Date(),false,false,false,false,null);
        orders = ordersService.creatOrders(orders);
        int orderId = orders.getId();
        orders.setOrderId(new Long(orderId));
        //遍历集合，依次在数据库中创建,并加在新建订单里
        Float sumPrice = orders.getSumPrice();
        for(OrderItem orderItem:orderItems){
            orderItem.setOrderId(new Long(orderId));
            //订单总价
            sumPrice += orderItem.getDishPrice();
            orderItemService.creatOrderItem(orderItem);
        }
        orders.setSumPrice(sumPrice);
        //更新orders(sumPrice)
        ordersService.creatOrders(orders);
        return ControllerUtil.getDataResult(orders);
    }

    @ApiOperation(value = "提交订单后设置预计取餐时间",httpMethod = "POST" )
    @ApiImplicitParams({
            @ApiImplicitParam(name = "orderId", value = "订单号", required = true),
            @ApiImplicitParam(name = "mealTime", value = "预计取餐时间(yyyy-MM-dd HH:mm)", required = true)
    })
    @PostMapping("/customerOrder/setMealTime")
    public ResponseVO setMealTime(String orderId,String mealTime){
        if(ordersService.setMealTime(orderId,mealTime)){
            return ControllerUtil.getSuccessResultBySelf("");
        }
        return ControllerUtil.getFalseResultMsgBySelf(RespCode.MSG_NOT_FOUND_DATA);
    }
}
