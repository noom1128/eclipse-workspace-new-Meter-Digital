using MySql.Data.MySqlClient;
using System;
using System.Collections.Generic;
using System.Data;
using System.IO;
using System.IO.Ports;
using System.Linq;
using System.Text;
using System.Threading;
using System.Windows.Forms;
using System.Xml;

namespace TTCReading.Class.ClsRead
{
    class JanitzaB21
    {
        ClsConnDB ConnDB = new ClsConnDB();
        ClsCRC16 CRC16 = new ClsCRC16();
        ClsComPort port = new ClsComPort();
        SerialPort ComPort = new SerialPort();
        string pathmeter = System.IO.Path.GetDirectoryName(System.Reflection.Assembly.GetExecutingAssembly().Location) + "\\ElectricRT\\";
        double[] datameter;
        string meterid = "";
        string meterName = "";
        string MeterAddr = "";
        string energytype = "";
        string dis = "";
        string chkerrorcon = "";
        string str = "";
        string[] str1;
        string chkdata = "";
        string por_type = "";

        double[] denominator = new double[31];
        private void setdenominator()
        {
            denominator[0] = 10;//VoltP1
            denominator[1] = 1;//VoltP2
            denominator[2] = 1;//VoltP3
            denominator[3] = 1;//VoltL1
            denominator[4] = 1;//VoltL2
            denominator[5] = 1;//VoltL3
            denominator[6] = 100;//Amp1
            denominator[7] = 1;//Amp2
            denominator[8] = 1;//Amp3
            denominator[9] = 1;
            denominator[10] = 100;//kW
            denominator[11] = 1;//kW1
            denominator[12] = 1;//kw2
            denominator[13] = 1;//kw3
            denominator[14] = 1;//kvar
            denominator[15] = 1;//kvar1
            denominator[16] = 1;//kvar2
            denominator[17] = 1;//kvar3
            denominator[18] = 100;//kva
            denominator[19] = 1;//kva1
            denominator[20] = 1;//kva2
            denominator[21] = 1;//kva3
            denominator[22] = 100;//Frequency
            denominator[23] = 1;//pf
            denominator[24] = 100;//kWh
            denominator[25] = 1;//
            denominator[26] = 1;//
            denominator[27] = 1;//kVArh
            denominator[28] = 1;//voltavg
            denominator[29] = 1;//ampavg
            denominator[30] = 1;//
        }

        #region WriteMeter
        public void readmeterB21(DataTable tb)
        {
            foreach (DataRow dr in tb.Rows)
            {
                meterid = dr["meter_id"].ToString();
                meterName = dr["meter_name"].ToString();
                MeterAddr = dr["meter_addr"].ToString();
                energytype = dr["energy_type"].ToString();
                dis = dr["meter_dis"].ToString();
                por_type = dr["por_type"].ToString();
                switch (por_type)
                {
                    case "1":
                        port.cboPorts = dr["por_comport"].ToString();
                        port.cboBaudRate = dr["por_buadrate"].ToString();
                        port.cboDataBits = dr["por_databit"].ToString();
                        port.cboParity = dr["por_parity"].ToString();
                        port.cboStopBits = dr["por_stopbit"].ToString();
                        ComPort = port.connectSP();
                        break;
                    case "2":

                        break;
                    case "3":

                        break;
                }
                try
                {
                    Array ar = new string[2] { "5B00", "5B3A" };
                    int cloop = 88;
                    double[] MakeDword = new double[31];
                    int c = 0;
                    int[] bytes = { 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 2, 2, 2 };
                    foreach (string hex1 in ar)
                    {
                        if (hex1 == "5B3A") cloop = 2;
                        if (chkerrorcon == "")
                        {
                            str = readdatameter32byte(Convert.ToInt32(MeterAddr), Int32.Parse(hex1.ToString(), System.Globalization.NumberStyles.HexNumber));
                            str1 = str.Split(',');
                            for (int i = 0; i < cloop; i += bytes[c])
                            {
                                byte[] ReData = new byte[4];
                                ReData[0] = 0x00;
                                ReData[1] = 0x00;
                                ReData[2] = Convert.ToByte(str1[i + 2]);
                                ReData[3] = Convert.ToByte(str1[i + 3]);
                                MakeDword[c] = CRC16.Read(ReData);
                                c++;
                            }
                        }
                    }
                    if (chkerrorcon == "")
                    {
                        if (str != "")
                        {
                            str = readdatameter4byte(Convert.ToInt32(MeterAddr), Int32.Parse("5008", System.Globalization.NumberStyles.HexNumber));
                            str1 = str.Split(',');
                            for (int a = 0; a < 40; a += 8)
                            {
                                byte[] ReData = new byte[4];
                                ReData[0] = 0x00;
                                ReData[1] = 0x00;
                                ReData[2] = Convert.ToByte(str1[a + 6]);
                                ReData[3] = Convert.ToByte(str1[a + 7]);
                                MakeDword[c] = CRC16.Read(ReData);
                                c++;
                            }
                        }
                    }
                    MakeDword[30] = Convert.ToInt32(str1[1]);
                    setdenominator();
                    datameter = new double[31];
                    for (int l = 0; l < 31; l++)
                    {
                        datameter[l] = Convert.ToDouble(MakeDword[l]) / Convert.ToDouble(denominator[l]);
                    }

                    if (datameter[14] > 32768)//kvar
                    {
                        datameter[14] = (65535 - datameter[14]) * -1;
                        datameter[14] /= 100;
                    }
                    if (datameter[27] > 32768)//kVArh
                    {
                        datameter[27] = (65535 - datameter[27]) * -1;
                        datameter[27] /= 100;
                    }
                    datameter[29] = (datameter[0] + datameter[1] + datameter[2]) / 3;//voltavg
                    datameter[30] = (datameter[6] + datameter[7] + datameter[8]) / 3;//ampavg
                    B21();
                    port.closeSP(ComPort);
                }
                catch (Exception) { }
            }
        }
        #endregion WriteMeter

        #region ReadMeter
        private string readdatameter4byte(int addr, Int64 command)
        {
            string valuemeter = "";
            Int64 GethightByte = command / 256;
            Int64 GetlowByte = command % 256;
            byte[] Sent_buff = new byte[8];
            int[] crc_result = new int[2];

            Sent_buff[0] = (byte)addr;
            Sent_buff[1] = 0x03;
            Sent_buff[2] = (byte)GethightByte;
            Sent_buff[3] = (byte)GetlowByte;
            Sent_buff[4] = 0x00;
            Sent_buff[5] = 0x20;

            crc_result = CRC16.crc16(Sent_buff, 6);

            Sent_buff[6] = (byte)crc_result[0];
            Sent_buff[7] = (byte)crc_result[1];
            // Send the one character buffer.
            ComPort.Write(Sent_buff, 0, Sent_buff.Length);
            Thread.Sleep(10);

            byte[] data = new byte[45];
            int offset = 0;
            int remaining = data.Length;
            while (remaining > 0)
            {
                try
                {
                    int read = ComPort.Read(data, offset, remaining);
                    if (read <= 0) { remaining = 0; }
                    else
                    {
                        remaining -= read;
                        offset += read;
                    }
                }
                catch (Exception) { chkdata = ""; break; }
            }
            if (chkdata == "")
            {
                byte[] ReData = new byte[40];
                for (int c = 0; c < 40; c++)
                {
                    ReData[c] = data[c + 3];
                    valuemeter += ReData[c].ToString() + ",";
                }
            }
            return valuemeter;
        }
        private string readdatameter32byte(int addr, Int64 command)
        {
            string date = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss", new System.Globalization.CultureInfo("en-US"));
            string valuemeter = "";
            byte[] Sent_buff = new byte[8];

            Int64 GethightByte = command / 256;
            Int64 GetlowByte = command % 256;
            int[] crc_result = new int[2];

            Sent_buff[0] = (byte)addr;
            Sent_buff[1] = 0x03;
            Sent_buff[2] = (byte)GethightByte;
            Sent_buff[3] = (byte)GetlowByte;
            Sent_buff[4] = 0x00;
            Sent_buff[5] = 0x32;

            crc_result = CRC16.crc16(Sent_buff, 6);

            Sent_buff[6] = (byte)crc_result[0];
            Sent_buff[7] = (byte)crc_result[1];
            // Send the one character buffer.
            ComPort.Write(Sent_buff, 0, Sent_buff.Length);
            Thread.Sleep(10);

            byte[] data = new byte[105];
            int offset = 0;
            int remaining = data.Length;
            while (remaining > 0)
            {
                try
                {
                    int read = ComPort.Read(data, offset, remaining);
                    if (read <= 0) { remaining = 0; }
                    else
                    {
                        remaining -= read;
                        offset += read;
                    }
                    if (Convert.ToBoolean(dis) == true)
                    {
                        bool connect = ConnDB.connect();
                        if (connect)
                        {
                            ConnDB.sql = "UPDATE eg_ms_meter " +
                            "SET meter_dis = @meter_dis, meter_rec_date = @meter_rec_date, meter_rec_status = @meter_rec_status " +
                            "WHERE meter_id = @meter_id ";
                            ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_dis", 0);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_rec_date", date);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_rec_status", "ADD");
                            ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                            ConnDB.reader.Close();
                        }
                        ConnDB.clossDB();
                    }
                }
                catch (Exception)
                {
                    chkdata = "";
                    if (Convert.ToBoolean(dis) == false)
                    {
                        bool connect = ConnDB.connect();
                        if (connect)
                        {
                            ConnDB.sql = "UPDATE eg_ms_meter " +
                            "SET meter_dis = @meter_dis, meter_rec_date = @meter_rec_date, meter_rec_status = @meter_rec_status " +
                            "WHERE meter_id = @meter_id ";
                            ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_dis", 1);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_rec_date", date);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_rec_status", "ACT");
                            ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                            ConnDB.reader.Close();
                        }
                        ConnDB.clossDB();
                    }
                    break;
                }
            }
            if (chkdata == "")
            {
                byte[] ReData = new byte[100];
                for (int c = 0; c < 100; c++)
                {
                    ReData[c] = data[c + 3];
                    valuemeter += ReData[c].ToString() + ",";
                }
            }
            return valuemeter;
        }
        #endregion ReadMeter

        #region Save
        private void B21()
        {
            try
            {
                DataTable tb = new DataTable();
                tb.Columns.Add("Begin", typeof(string));
                tb.Columns.Add("ID", typeof(string));
                tb.Columns.Add("Name", typeof(string));
                tb.Columns.Add("kW", typeof(float));
                tb.Columns.Add("kW1", typeof(float));
                tb.Columns.Add("kW2", typeof(float));
                tb.Columns.Add("kW3", typeof(float));
                tb.Columns.Add("kVAr", typeof(float));
                tb.Columns.Add("kWh", typeof(float));
                tb.Columns.Add("kVA", typeof(float));
                tb.Columns.Add("kVArh", typeof(float));
                tb.Columns.Add("VoltP1", typeof(float));
                tb.Columns.Add("VoltP2", typeof(float));
                tb.Columns.Add("VoltP3", typeof(float));
                tb.Columns.Add("VoltAvg", typeof(float));
                tb.Columns.Add("VoltL1", typeof(float));
                tb.Columns.Add("VoltL2", typeof(float));
                tb.Columns.Add("VoltL3", typeof(float));
                tb.Columns.Add("Amp1", typeof(float));
                tb.Columns.Add("Amp2", typeof(float));
                tb.Columns.Add("Amp3", typeof(float));
                tb.Columns.Add("AmpAvg", typeof(float));
                tb.Columns.Add("AmpN", typeof(float));
                tb.Columns.Add("pf", typeof(float));
                tb.Columns.Add("Frequency", typeof(float));

                string id = DateTime.Now.ToString("yyyyMMddHHmm", new System.Globalization.CultureInfo("en-US"));
                double[] data = datameter;
                DataRow dr = tb.NewRow();
                dr["Begin"] = id;
                dr["ID"] = meterid;
                dr["Name"] = meterName;
                dr["kW"] = data[10].ToString("0.00");
                dr["kW1"] = data[11].ToString("0.00");
                dr["kW2"] = data[12].ToString("0.00");
                dr["kW3"] = data[13].ToString("0.00");
                dr["kVAr"] = data[14].ToString("0.00");
                dr["kWh"] = data[24].ToString("0.00");
                dr["kVA"] = data[18].ToString("0.00");
                dr["kVArh"] = data[27].ToString("0.00");
                dr["VoltP1"] = data[0].ToString("0.00");
                dr["VoltP2"] = data[1].ToString("0.00");
                dr["VoltP3"] = data[2].ToString("0.00");
                dr["VoltAvg"] = data[29].ToString("0.00");
                dr["VoltL1"] = data[3].ToString("0.00");
                dr["VoltL2"] = data[4].ToString("0.00");
                dr["VoltL3"] = data[5].ToString("0.00");
                dr["Amp1"] = data[6].ToString("0.00");
                dr["Amp2"] = data[7].ToString("0.00");
                dr["Amp3"] = data[8].ToString("0.00");
                dr["AmpAvg"] = data[30].ToString("0.00");
                dr["AmpN"] = data[9].ToString("0.00");
                dr["pf"] = data[23].ToString("0.00");
                dr["Frequency"] = data[22].ToString("0.00");
                tb.Rows.Add(dr);
                if (Convert.ToBoolean(dis) == false)
                {
                    SaveRealTime(dr);
                    //Save_1min(dr);
                }
                else
                {
                    ErrorRealTime();
                }
            }
            catch (Exception ex) { MessageBox.Show("ReadMySQL JanitzaB21" + ex.Message); }
        }

        private bool chkrecord(string tb, string where)
        {
            bool connect = ConnDB.connect();
            bool chk = false;
            if (connect)
            {
                ConnDB.sql = "select * from " + tb + " " + where;
                ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                chk = ConnDB.reader.HasRows;
                ConnDB.reader.Close();
            }
            ConnDB.clossDB();
            return chk;
        }

        private void ErrorRealTime()
        {
            string Time = DateTime.Now.ToString("yyyyMMddHH", new System.Globalization.CultureInfo("en-US"));
            string MeterData = "0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";
            bool chkdemand = chkrecord("eg_tt_data", "where meter_id ='" + meterid + "'");
            bool connect = ConnDB.connect();
            if (connect)
            {
                if (!chkdemand)
                {
                    ConnDB.sql = "insert into eg_tt_data " +
                    "value(@meter_id, @eg_id, @energy_type, @eg_data)";
                }
                else
                {
                    ConnDB.sql = "update eg_tt_data " +
                    "set eg_id = @eg_id, energy_type = @energy_type, eg_data = @eg_data " +
                    "where meter_id = @meter_id ";
                }
                ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                ConnDB.mycomm.Parameters.AddWithValue("@eg_id", Time);
                ConnDB.mycomm.Parameters.AddWithValue("@energy_type", energytype);
                ConnDB.mycomm.Parameters.AddWithValue("@eg_data", MeterData);
                ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                ConnDB.reader.Close();
            }
            ConnDB.clossDB();
        }

        private void SaveRealTime(DataRow dr)
        {
            string Time = DateTime.Now.ToString("yyyyMMddHH", new System.Globalization.CultureInfo("en-US"));
            string MeterData = dr["kW"] + ",0,0,0," + dr["kVAr"] + "," + dr["kWh"] + "," + dr["kVA"] + "," + dr["kVArh"] + "," +
              dr["VoltP1"] + ",0,0,0,0,0,0," + dr["Amp1"] + ",0,0,0,0,0," + dr["frequency"] + ",0,0,0,0,0,0";
            bool chkdemand = chkrecord("eg_tt_data", "where meter_id ='" + meterid + "'");
            bool connect = ConnDB.connect();
            if (connect)
            {
                if (!chkdemand)
                {
                    ConnDB.sql = "insert into eg_tt_data " +
                    "value(@meter_id, @eg_id, @energy_type, @eg_data)";
                }
                else
                {
                    ConnDB.sql = "update eg_tt_data " +
                    "set eg_id = @eg_id, energy_type = @energy_type, eg_data = @eg_data " +
                    "where meter_id = @meter_id ";
                }
                ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                ConnDB.mycomm.Parameters.AddWithValue("@eg_id", Time);
                ConnDB.mycomm.Parameters.AddWithValue("@energy_type", energytype);
                ConnDB.mycomm.Parameters.AddWithValue("@eg_data", MeterData);
                ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                ConnDB.reader.Close();
            }
            ConnDB.clossDB();
        }

        private void Save_1min(DataRow dr)
        {
            DateTime now = DateTime.Now, date = new DateTime(now.Year, now.Month, now.Day, now.Hour, now.Minute, 0);
            string Time = date.ToString("yyyyMMddHH", new System.Globalization.CultureInfo("en-US"));
            string mm = date.ToString("mm", new System.Globalization.CultureInfo("en-US"));
            bool chkdemand = chkrecord("eg_tr_minutes", "where hour_id='" + Time + "' and meter_id ='" + meterid + "'");
            string value = "0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";
            string[] eg = new string[60];
            eg[00] = value; eg[01] = value; eg[02] = value; eg[03] = value; eg[04] = value; eg[05] = value; eg[06] = value; eg[07] = value; eg[08] = value; eg[09] = value;
            eg[10] = value; eg[11] = value; eg[12] = value; eg[13] = value; eg[14] = value; eg[15] = value; eg[16] = value; eg[17] = value; eg[18] = value; eg[19] = value;
            eg[20] = value; eg[21] = value; eg[22] = value; eg[23] = value; eg[24] = value; eg[25] = value; eg[26] = value; eg[27] = value; eg[28] = value; eg[29] = value;
            eg[30] = value; eg[31] = value; eg[32] = value; eg[33] = value; eg[34] = value; eg[35] = value; eg[36] = value; eg[37] = value; eg[38] = value; eg[39] = value;
            eg[40] = value; eg[41] = value; eg[42] = value; eg[43] = value; eg[44] = value; eg[45] = value; eg[46] = value; eg[47] = value; eg[48] = value; eg[49] = value;
            eg[50] = value; eg[51] = value; eg[52] = value; eg[53] = value; eg[54] = value; eg[55] = value; eg[56] = value; eg[57] = value; eg[58] = value; eg[59] = value;

            string MeterData = dr["kW"] + ",0,0,0," + dr["kVAr"] + "," + dr["kWh"] + "," + dr["kVA"] + "," + dr["kVArh"] + "," +
              dr["VoltP1"] + ",0,0,0,0,0,0," + dr["Amp1"] + ",0,0,0,0,0," + dr["frequency"] + ",0,0,0,0,0,0";
            bool connect = ConnDB.connect();
            if (connect)
            {
                if (!chkdemand)
                {
                    ConnDB.sql = "insert into eg_tr_minutes " +
                    "values (@hour_id,@meter_id,@energy_type,'" +
                    eg[00] + "','" + eg[01] + "','" + eg[02] + "','" + eg[03] + "','" + eg[04] + "','" + eg[05] + "','" + eg[06] + "','" + eg[07] + "','" + eg[08] + "','" + eg[09] + "','" +
                    eg[10] + "','" + eg[11] + "','" + eg[12] + "','" + eg[13] + "','" + eg[14] + "','" + eg[15] + "','" + eg[16] + "','" + eg[17] + "','" + eg[18] + "','" + eg[19] + "','" +
                    eg[20] + "','" + eg[21] + "','" + eg[22] + "','" + eg[23] + "','" + eg[24] + "','" + eg[25] + "','" + eg[26] + "','" + eg[27] + "','" + eg[28] + "','" + eg[29] + "','" +
                    eg[30] + "','" + eg[31] + "','" + eg[32] + "','" + eg[33] + "','" + eg[34] + "','" + eg[35] + "','" + eg[36] + "','" + eg[37] + "','" + eg[38] + "','" + eg[39] + "','" +
                    eg[40] + "','" + eg[41] + "','" + eg[42] + "','" + eg[43] + "','" + eg[44] + "','" + eg[45] + "','" + eg[46] + "','" + eg[47] + "','" + eg[48] + "','" + eg[49] + "','" +
                    eg[50] + "','" + eg[51] + "','" + eg[52] + "','" + eg[53] + "','" + eg[54] + "','" + eg[55] + "','" + eg[56] + "','" + eg[57] + "','" + eg[58] + "','" + eg[59] + "')";
                }
                else
                {
                    ConnDB.sql = "update eg_tr_minutes set eg_" + mm + "='" + MeterData + "' where hour_id=@hour_id and meter_id=@meter_id ";
                }
                ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                ConnDB.mycomm.Parameters.AddWithValue("@hour_id", Time);
                ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                ConnDB.mycomm.Parameters.AddWithValue("@energy_type", energytype);
                ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                ConnDB.reader.Close();
            }
            ConnDB.clossDB();
        }

        private void Save_15min(DataTable tb)
        {
            DateTime now = DateTime.Now, date = new DateTime(now.Year, now.Month, now.Day, now.Hour, 0, 0);
            DateTime minute = date.AddMinutes((now.Minute / 15) * 15);
            string Time = minute.ToString("yyyyMMddHH", new System.Globalization.CultureInfo("en-US"));
            string mm = minute.ToString("mm", new System.Globalization.CultureInfo("en-US"));
            string time_begin = minute.AddMinutes(-15).ToString("yyyyMMddHHmm", new System.Globalization.CultureInfo("en-US"));
            string time_final = minute.ToString("yyyyMMddHHmm", new System.Globalization.CultureInfo("en-US"));
            bool chkdemand = chkrecord("eg_tr_demand", "where hour_id='" + Time + "' and meter_id ='" + meterid + "'");
            DataRow[] begin, final;
            final = tb.Select("Begin = " + time_final);
            double unit = 0;
            foreach (DataRow dr in final)
            {
                begin = tb.Select("Begin =" + time_begin + "AND ID = " + dr[1]);
                double kwh_begin = begin.Length != 0 ? 0 : Convert.ToDouble(dr["kWh"]);//Convert.ToDouble(begin[0]["kWh"])
                unit = Convert.ToDouble(dr["kWh"]) - kwh_begin;

                string value = "0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";
                string[] eg = new string[4];
                eg[0] = value; eg[1] = value; eg[2] = value; eg[3] = value;

                string MeterData = dr["kW"] + "," + dr["kW1"] + "," + dr["kW2"] + "," + dr["kW3"] + "," + dr["kVAr"] + "," + kwh_begin.ToString("0.00") + "," + dr["kWh"] + "," + unit + "," +
                  dr["kVA"] + "," + dr["kVArh"] + "," + dr["VoltP1"] + "," + dr["VoltP2"] + "," + dr["VoltP3"] + "," + dr["VoltL1"] + "," + dr["VoltL2"] + "," + dr["VoltL3"] + "," +
                  dr["Amp1"] + "," + dr["Amp2"] + "," + dr["Amp3"] + "," + dr["AmpN"] + "," + dr["pf"] + "," + dr["frequency"] + ",0,0,0,0,0,0";

                bool connect = ConnDB.connect();
                if (connect)
                {
                    if (!chkdemand)
                    {
                        ConnDB.sql = "insert into eg_tr_demand " +
                        "values (@hour_id,@meter_id,@energy_type,'" + eg[0] + "','" + eg[1] + "','" + eg[2] + "','" + eg[3] + "');";
                    }
                    else
                    {
                        ConnDB.sql = "update eg_tr_demand set eg_" + mm + "='" + MeterData + "' where hour_id=@hour_id and meter_id=@meter_id";
                    }
                    ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                    ConnDB.mycomm.Parameters.AddWithValue("@hour_id", Time);
                    ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                    ConnDB.mycomm.Parameters.AddWithValue("@energy_type", energytype);
                    ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                    ConnDB.reader.Close();
                }
                ConnDB.clossDB();
            }
        }

        private void Save_1hour(DataTable tb)
        {
            DateTime now = DateTime.Now;
            DateTime date = new DateTime(now.Year, now.Month, now.Day, now.Hour, 0, 0);
            string Time = date.ToString("yyyyMMdd", new System.Globalization.CultureInfo("en-US"));
            string HH = date.ToString("HH", new System.Globalization.CultureInfo("en-US"));
            string time_begin = date.AddHours(-1).ToString("yyyyMMddHHmm", new System.Globalization.CultureInfo("en-US"));
            string time_final = date.ToString("yyyyMMddHHmm", new System.Globalization.CultureInfo("en-US"));
            bool chkdemand = chkrecord("eg_tr_hour", "where day_id='" + Time + "' and meter_id ='" + meterid + "'");
            DataRow[] begin, final;
            final = tb.Select("Begin = " + time_final);
            double unit = 0;
            foreach (DataRow dr in final)
            {
                begin = tb.Select("Begin >=" + time_begin + "AND Begin < " + time_final + "AND ID = " + dr[1]);
                double kwh_begin = begin.Length != 0 ? 0 : Convert.ToDouble(dr["kWh"]);//Convert.ToDouble(begin[0]["kWh"])
                unit = Convert.ToDouble(dr["kWh"]) - kwh_begin;

                string value = "0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";
                string[] eg = new string[24];
                eg[0] = value; eg[1] = value; eg[2] = value; eg[3] = value; eg[4] = value; eg[5] = value;
                eg[6] = value; eg[7] = value; eg[8] = value; eg[9] = value; eg[10] = value; eg[11] = value;
                eg[12] = value; eg[13] = value; eg[14] = value; eg[15] = value; eg[16] = value; eg[17] = value;
                eg[18] = value; eg[19] = value; eg[20] = value; eg[21] = value; eg[22] = value; eg[23] = value;

                string MeterData = dr["kW"] + "," + dr["kW1"] + "," + dr["kW2"] + "," + dr["kW3"] + "," + dr["kVAr"] + "," + kwh_begin.ToString("0.00") + "," + dr["kWh"] + "," + unit + "," +
                  dr["kVA"] + "," + dr["kVArh"] + "," + dr["VoltP1"] + "," + dr["VoltP2"] + "," + dr["VoltP3"] + "," + dr["VoltL1"] + "," + dr["VoltL2"] + "," + dr["VoltL3"] + "," +
                  dr["Amp1"] + "," + dr["Amp2"] + "," + dr["Amp3"] + "," + dr["AmpN"] + "," + dr["pf"] + "," + dr["frequency"] + ",0,0,0,0,0,0";
                bool connect = ConnDB.connect();
                if (connect)
                {
                    if (!chkdemand)
                    {
                        ConnDB.sql = "insert into eg_tr_hour " +
                        "values (@day_id,@meter_id,@energy_type,'" +
                        eg[0] + "','" + eg[1] + "','" + eg[2] + "','" + eg[3] + "','" + eg[4] + "','" + eg[5] + "','" + eg[6] + "','" + eg[7] + "','" +
                        eg[8] + "','" + eg[9] + "','" + eg[10] + "','" + eg[11] + "','" + eg[12] + "','" + eg[13] + "','" + eg[14] + "','" + eg[15] + "','" +
                        eg[16] + "','" + eg[17] + "','" + eg[18] + "','" + eg[19] + "','" + eg[20] + "','" + eg[21] + "','" + eg[22] + "','" + eg[23] + "');";
                    }
                    else
                    {
                        ConnDB.sql = "update eg_tr_hour set eg_" + HH + "='" + MeterData + "' where day_id=@day_id and meter_id=@meter_id";
                    }
                    ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                    ConnDB.mycomm.Parameters.AddWithValue("@day_id", Time);
                    ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                    ConnDB.mycomm.Parameters.AddWithValue("@energy_type", energytype);
                    ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                    ConnDB.reader.Close();
                }
                ConnDB.clossDB();
            }
        }

        private void XMLB21()
        {
            if (File.Exists(pathmeter + meterName + ".xml") == false)
            {
                XmlTextWriter writer = new XmlTextWriter(pathmeter + meterName + ".xml", System.Text.Encoding.UTF8);
                writer.WriteStartDocument(true);
                writer.Formatting = Formatting.Indented;
                writer.Indentation = 2;
                writer.WriteStartElement("ReadMeter");
                B21(writer);
                writer.WriteEndElement();
                writer.WriteEndDocument();
                writer.Close();
            }
            else { UpdateB21(); }
        }

        private void B21(XmlTextWriter writer)
        {
            try
            {
                double[] data = datameter;
                writer.WriteStartElement("DateTime");
                writer.WriteString(DateTime.Now.ToString("dd/MM/yyyy HH:mm:ss", new System.Globalization.CultureInfo("en-US")));
                writer.WriteEndElement();
                writer.WriteStartElement("MeterID");
                writer.WriteString(meterid);
                writer.WriteEndElement();
                writer.WriteStartElement("Name");
                writer.WriteString(meterName);
                writer.WriteEndElement();
                writer.WriteStartElement("Address");
                writer.WriteString(MeterAddr);
                writer.WriteEndElement();
                writer.WriteStartElement("EnergyType");
                writer.WriteString(energytype);
                writer.WriteEndElement();
                writer.WriteStartElement("kW");
                writer.WriteString(data[10].ToString("0.00"));
                writer.WriteEndElement();
                writer.WriteStartElement("kW1");
                writer.WriteString(data[11].ToString("0.00"));
                writer.WriteEndElement();
                writer.WriteStartElement("kW2");
                writer.WriteString(data[12].ToString("0.00"));
                writer.WriteEndElement();
                writer.WriteStartElement("kW3");
                writer.WriteString(data[13].ToString("0.00"));
                writer.WriteEndElement();
                writer.WriteStartElement("kVAr");
                writer.WriteString(data[14].ToString("0.00"));
                writer.WriteEndElement();
                writer.WriteStartElement("kWh");
                writer.WriteString(data[24].ToString("0.00"));
                writer.WriteEndElement();
                writer.WriteStartElement("kVA");
                writer.WriteString(data[18].ToString("0.00"));
                writer.WriteEndElement();
                writer.WriteStartElement("kVArh");
                writer.WriteString(data[27].ToString("0.00"));
                writer.WriteEndElement();
                writer.WriteStartElement("VoltP1");
                writer.WriteString(data[0].ToString("0.00"));
                writer.WriteEndElement();
                writer.WriteStartElement("VoltP2");
                writer.WriteString(data[1].ToString("0.00"));
                writer.WriteEndElement();
                writer.WriteStartElement("VoltP3");
                writer.WriteString(data[2].ToString("0.00"));
                writer.WriteEndElement();
                writer.WriteStartElement("VoltAvg");
                writer.WriteString(data[29].ToString("0.00"));
                writer.WriteEndElement();
                writer.WriteStartElement("VoltL1");
                writer.WriteString(data[3].ToString("0.00"));
                writer.WriteEndElement();
                writer.WriteStartElement("VoltL2");
                writer.WriteString(data[4].ToString("0.00"));
                writer.WriteEndElement();
                writer.WriteStartElement("VoltL3");
                writer.WriteString(data[5].ToString("0.00"));
                writer.WriteEndElement();
                writer.WriteStartElement("Amp1");
                writer.WriteString(data[6].ToString("0.00"));
                writer.WriteEndElement();
                writer.WriteStartElement("Amp2");
                writer.WriteString(data[7].ToString("0.00"));
                writer.WriteEndElement();
                writer.WriteStartElement("Amp3");
                writer.WriteString(data[8].ToString("0.00"));
                writer.WriteEndElement();
                writer.WriteStartElement("AmpAvg");
                writer.WriteString(data[30].ToString("0.00"));
                writer.WriteEndElement();
                writer.WriteStartElement("AmpN");
                writer.WriteString(data[9].ToString("0.00"));
                writer.WriteEndElement();
                writer.WriteStartElement("Pf");
                writer.WriteString(data[23].ToString("0.00"));
                writer.WriteEndElement();
                writer.WriteStartElement("Frequency");
                writer.WriteString(data[22].ToString("0.00"));
                writer.WriteEndElement();
            }
            catch (Exception e) { MessageBox.Show("ReadXML !!" + e.Message); }
        }

        private void UpdateB21()
        {
            try
            {
                double[] data = datameter;
                string pathdatameter = pathmeter + meterName + ".xml";
                DataSet ds = new DataSet();
                ds.ReadXml(pathdatameter);
                ds.Tables[0].Rows[0]["DateTime"] = DateTime.Now.ToString("dd/MM/yyyy HH:mm:ss", new System.Globalization.CultureInfo("en-US"));
                ds.Tables[0].Rows[0]["MeterID"] = meterid;
                ds.Tables[0].Rows[0]["Name"] = meterName;
                ds.Tables[0].Rows[0]["Address"] = MeterAddr;
                ds.Tables[0].Rows[0]["EnergyType"] = energytype;
                ds.Tables[0].Rows[0]["kW"] = data[10].ToString("0.00");
                ds.Tables[0].Rows[0]["kW1"] = data[11].ToString("0.00");
                ds.Tables[0].Rows[0]["kW2"] = data[12].ToString("0.00");
                ds.Tables[0].Rows[0]["kW3"] = data[13].ToString("0.00");
                ds.Tables[0].Rows[0]["kVAr"] = data[14].ToString("0.00");
                ds.Tables[0].Rows[0]["kWh"] = data[24].ToString("0.00");
                ds.Tables[0].Rows[0]["kVA"] = data[18].ToString("0.00");
                ds.Tables[0].Rows[0]["kVArh"] = data[27].ToString("0.00");
                ds.Tables[0].Rows[0]["VoltP1"] = data[0].ToString("0.00");
                ds.Tables[0].Rows[0]["VoltP2"] = data[1].ToString("0.00");
                ds.Tables[0].Rows[0]["VoltP3"] = data[2].ToString("0.00");
                ds.Tables[0].Rows[0]["VoltAvg"] = data[29].ToString("0.00");
                ds.Tables[0].Rows[0]["VoltL1"] = data[3].ToString("0.00");
                ds.Tables[0].Rows[0]["VoltL2"] = data[4].ToString("0.00");
                ds.Tables[0].Rows[0]["VoltL3"] = data[5].ToString("0.00");
                ds.Tables[0].Rows[0]["Amp1"] = data[6].ToString("0.00");
                ds.Tables[0].Rows[0]["Amp2"] = data[7].ToString("0.00");
                ds.Tables[0].Rows[0]["Amp3"] = data[8].ToString("0.00");
                ds.Tables[0].Rows[0]["AmpAvg"] = data[30].ToString("0.00");
                ds.Tables[0].Rows[0]["AmpN"] = data[9].ToString("0.00");
                ds.Tables[0].Rows[0]["Pf"] = data[23].ToString("0.00");
                ds.Tables[0].Rows[0]["Frequency"] = data[22].ToString("0.00");
                ds.WriteXml(pathdatameter);
            }
            catch (Exception) { }
        }
        #endregion Save
    }
}
