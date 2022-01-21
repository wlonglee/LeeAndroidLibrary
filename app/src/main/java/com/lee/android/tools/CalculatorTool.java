package com.lee.android.tools;

import java.math.BigDecimal;

/**
 * @author lee
 * @date 2021/9/10
 */
public class CalculatorTool {

    public static void main(String[] args) {
        CalculatorData data = new CalculatorData();

//        data.holdPrice=4.9101f;
//        data.holdNum=1500;
//        int sell=700;
//        data.calculatorSell(4.7f, sell)
//                .calculatorSell(4.72f, sell)
//                .calculatorBuy(4.5f, 1600)
//        ;

        data.holdPrice =22.7f;
        data.holdNum = 200;
        data.totalBuyPrice = data.holdPrice * data.holdNum;

        Log("持有单价", data.holdPrice);
        Log("持有数", data.holdNum);
        Log("");

        data
//                .calculatorSell(8.09f, 600)
//                .calculatorSell(12.8f, 300)
//                .calculatorBuy(3.4f, 1000)
//                .calculatorBuy(3.3f, 1000)
                .calculatorBuy(20.4f, 400)
//                .calculatorBuy(20f, 200)
//                .calculatorBuy(11.6f, 500)
//                .calculatorBuy(12.08f, 100)
//                .calculatorSell(8f, 900)
//                .calculatorSell(4.9f, 200)
        ;
    }


    //印花
    private static final float PRINTING = 0.001f;

    //过户
    private static final float TRANSFER = 0.00002f;

    //经手
    private static final float DEAL = 0.0000487f;

    //证管
    private static final float WITH = 0.00002f;

    //买入过5000额外费率
    private static final float EXTRA_CAST = 0.00021875f;

    public static class CalculatorData {
        //持有单价
        public float holdPrice = 0f;
        //持有数量
        public int holdNum = 0;


        public float totalBuyPrice = 0f;
        private float totalSellPrice = 0f;

        //印花
        public float printing = 0f;
        //过户
        public float transfer;
        //经手
        public float deal;
        //证管
        public float with;

        //计算买入成本
        public CalculatorData calculatorBuy(float price, int num) {
            float totalPrice = price * num;
            transfer = BigDecimal.valueOf(totalPrice * TRANSFER).setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();
            deal = BigDecimal.valueOf(totalPrice * DEAL).setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();
            with = BigDecimal.valueOf(totalPrice * WITH).setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();

            float poundage = BigDecimal.valueOf(5f - transfer - deal).setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();
            if (totalPrice > 5000) {
                float extraPoundage = BigDecimal.valueOf(totalPrice * EXTRA_CAST).setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();
                poundage = BigDecimal.valueOf(poundage + extraPoundage).setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();
            }
            float totalPoundage = BigDecimal.valueOf(poundage + transfer + deal + with).setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();

            float buyCastPrice = BigDecimal.valueOf(totalPrice + totalPoundage).setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();
            float buyPrice = BigDecimal.valueOf(buyCastPrice / num).setScale(4, BigDecimal.ROUND_HALF_UP).floatValue();

            holdPrice = BigDecimal.valueOf((holdNum * holdPrice + buyCastPrice) / (holdNum + num)).setScale(4, BigDecimal.ROUND_HALF_UP).floatValue();
            holdNum += num;
            Log("买入单价", price);
            Log("买入数量", num);
//            Log("总价", totalPrice);
//            Log("过户费", transfer);
//            Log("经手费", deal);
//            Log("证管费", with);
//            Log("净手续费", poundage);
            Log("总手续费", totalPoundage);
            Log("买入花费", buyCastPrice);
//            Log("到手成本", buyPrice);
            Log("当前持有数量", holdNum);
            Log("当前持有单价", holdPrice);

            totalBuyPrice = BigDecimal.valueOf(totalBuyPrice + buyCastPrice).setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();
//            Log("累计买入总花费", totalBuyPrice);
            Log("");
            return this;
        }

        //计算卖出收益
        public CalculatorData calculatorSell(float price, int num) {
            float totalPrice = price * num;
            printing = BigDecimal.valueOf(totalPrice * PRINTING).setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();
            transfer = BigDecimal.valueOf(totalPrice * TRANSFER).setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();
            deal = BigDecimal.valueOf(totalPrice * DEAL).setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();
            with = BigDecimal.valueOf(totalPrice * WITH).setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();

            float poundage;
            if (printing > 6) {
                poundage = BigDecimal.valueOf(totalPrice * PRINTING - transfer - deal - with).setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();
            } else if (printing > 5) {
                poundage = BigDecimal.valueOf(totalPrice * PRINTING - transfer - deal).setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();
            } else {
                poundage = BigDecimal.valueOf(5f - transfer - deal).setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();
            }

            float totalPoundage = BigDecimal.valueOf(printing + poundage + transfer + deal + with).setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();
            float sellPrice = BigDecimal.valueOf(totalPrice - totalPoundage).setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();

            float profit = BigDecimal.valueOf((price - holdPrice) * num - totalPoundage).setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();
            holdNum -= num;

            totalSellPrice = BigDecimal.valueOf(totalSellPrice + sellPrice).setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();
//            Log("累计卖出", totalSellPrice);

            if (holdNum > 0) {
                if (profit < 0) {
                    //亏本跳车, 成本需要叠加到剩下的上边去
                    holdPrice = BigDecimal.valueOf(holdPrice + Math.abs((price - holdPrice) * num - totalPoundage) / holdNum).setScale(4, BigDecimal.ROUND_HALF_UP).floatValue();
                } else {
                    holdPrice = BigDecimal.valueOf(holdPrice - ((price - holdPrice) * num - totalPoundage) / holdNum).setScale(4, BigDecimal.ROUND_HALF_UP).floatValue();
                }
                Log("卖出单价", price);
                Log("卖出数量", num);
//                Log("印花税", printing);
//                Log("过户费", transfer);
//                Log("经手费", deal);
//                Log("证管费", with);
//                Log("净手续费", poundage);
                Log("总手续费", totalPoundage);
//                Log("卖出金额", sellPrice);
                Log("卖出盈利", profit);
                Log("当前持有数量", holdNum);
                Log("当前持有单价", holdPrice);
            } else {
                //清仓了
                Log("清仓了");
                Log("卖出单价", price);
                Log("卖出数量", num);
//                Log("总价", totalPrice);
//                Log("印花税", printing);
//                Log("过户费", transfer);
//                Log("经手费", deal);
//                Log("证管费", with);
//                Log("净手续费", poundage);
                Log("总手续费", totalPoundage);
//                Log("卖出金额", sellPrice);

                Log("清仓盈利", BigDecimal.valueOf(totalSellPrice - totalBuyPrice).setScale(2, BigDecimal.ROUND_HALF_UP).floatValue());
            }


            Log("");
            return this;
        }


    }


    public static void Log(Object obj) {
        System.out.println(obj.toString());
    }

    public static void Log(String tag, Object obj) {
        System.out.println(tag + ":" + obj.toString());
    }
}
