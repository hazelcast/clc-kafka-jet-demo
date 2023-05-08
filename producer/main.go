package main

import (
	"context"
	"encoding/json"
	"flag"
	"fmt"
	"math/rand"
	"os/exec"
	"strconv"
	"time"

	"github.com/hazelcast/hazelcast-go-client"
	"github.com/hazelcast/hazelcast-go-client/logger"
)

var (
	mapName   = flag.String("n", "desserts", "Map Name")
	address   = flag.String("ad", "localhost:5701", "Address of cluster")
	topic     = flag.String("t", "orders", "Topic Name")
	perSecond = flag.Int("per", 1, "Number of produced items per second")
)

type order struct {
	id        int
	DessertID int `json:"dessertID"`
	Count     int `json:"count"`
}

func main() {
	flag.Parse()
	ctx := context.TODO()

	config := hazelcast.Config{}
	config.Logger.Level = logger.DebugLevel
	config.Cluster.Network.SetAddresses(*address)
	client, err := hazelcast.StartNewClientWithConfig(ctx, config)
	if err != nil {
		panic(err)
	}

	m, err := client.GetMap(ctx, *mapName)
	if err != nil {
		panic(err)
	}

	desserts, err := m.GetEntrySet(ctx)
	if err != nil {
		panic(err)
	}
	err = client.Shutdown(ctx)
	if err != nil {
		panic(err)
	}

	for {
		o := order{
			DessertID: rand.Intn(len(desserts)),
			id:        rand.Intn(1_000_000),
			Count:     rand.Intn(5) + 1,
		}
		b, _ := json.Marshal(o)
		_, err := exec.Command("kafkactl", "produce", *topic,
			"-k", strconv.Itoa(o.id), "-v", string(b)).Output()
		if err != nil {
			fmt.Println(err)
		} else {
			fmt.Println("Generated", "OrderID", o.id, "DessertID", o.DessertID, "Count", o.Count)
		}
		time.Sleep(1 * time.Second / time.Duration(*perSecond))
	}
}
